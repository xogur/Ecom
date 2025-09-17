package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.*;
import com.ecommerce.project.payload.*;
import com.ecommerce.project.repositories.*;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import static com.ecommerce.project.model.QOrder.order;
import static com.ecommerce.project.model.QOrderItem.orderItem;
import static com.ecommerce.project.model.QProduct.product;
import static com.ecommerce.project.model.QPayment.payment;
import static com.ecommerce.project.model.QAddress.address;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    @Autowired
    CartRepository cartRepository;

    @Autowired
    AddressRepository addressRepository;

    @Autowired
    OrderItemRepository orderItemRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    CartService cartService;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    ProductRepository productRepository;

    private final JPAQueryFactory queryFactory;

    @Override
    @Transactional
    public OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage) {
        Cart cart = cartRepository.findCartByEmail(emailId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "email", emailId);
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        Order order = new Order();
        order.setEmail(emailId);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setOrderStatus("Order Accepted !");
        order.setAddress(address);

        Payment payment = new Payment(paymentMethod, pgPaymentId, pgStatus, pgResponseMessage, pgName);
        payment.setOrder(order);
        payment = paymentRepository.save(payment);
        order.setPayment(payment);

        Order savedOrder = orderRepository.save(order);

        List<CartItem> cartItems = cart.getCartItems();
        if (cartItems.isEmpty()) {
            throw new APIException("Cart is empty");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setOrderedProductPrice(cartItem.getProductPrice());
            orderItem.setOrder(savedOrder);
            orderItems.add(orderItem);
        }

        orderItems = orderItemRepository.saveAll(orderItems);

        cart.getCartItems().forEach(item -> {
            int quantity = item.getQuantity();
            Product product = item.getProduct();

            // Reduce stock quantity
            product.setQuantity(product.getQuantity() - quantity);

            // Save product back to the database
            productRepository.save(product);

            // Remove items from cart
            cartService.deleteProductFromCart(cart.getCartId(), item.getProduct().getProductId());
        });

        OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);
        orderItems.forEach(item -> orderDTO.getOrderItems().add(modelMapper.map(item, OrderItemDTO.class)));

        orderDTO.setAddressId(addressId);

        return orderDTO;
    }


    @Override
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(order -> new OrderDTO(
                        order.getOrderId(),
                        order.getEmail(),
                        order.getOrderItems().stream()
                                .map(item -> new OrderItemDTO(
                                        item.getOrderItemId(),
                                        new ProductDTO(
                                                item.getProduct().getProductId(),
                                                item.getProduct().getProductName(),
                                                item.getProduct().getImage(),
                                                item.getProduct().getDescription(),
                                                item.getProduct().getQuantity(),
                                                item.getProduct().getPrice(),
                                                item.getProduct().getDiscount(),
                                                item.getProduct().getSpecialPrice()
                                        ),
                                        item.getQuantity(),
                                        item.getDiscount(),
                                        item.getOrderedProductPrice()
                                ))
                                .collect(Collectors.toList()),
                        order.getOrderDate(),
                        new PaymentDTO(
                                order.getPayment().getPaymentId(),
                                order.getPayment().getPaymentMethod(),
                                order.getPayment().getPgStatus()
                        ),
                        order.getTotalAmount(),
                        order.getOrderStatus(),
                        order.getAddress().getAddressId(),
                        new AddressDTO(
                                order.getAddress().getAddressId(),
                                order.getAddress().getStreet(),
                                order.getAddress().getBuildingName(),
                                order.getAddress().getCity(),
                                order.getAddress().getState(),
                                order.getAddress().getCountry(),
                                order.getAddress().getPincode()
                        )
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<OrderDTO> getUserOrders(String userEmail) {

        // 1) 한 번의 조인 쿼리로 주문/아이템/상품/결제/주소를 모두 가져온다.
        //    (아이템이 없는 주문도 나와야 하므로 left join)
        List<Tuple> rows = queryFactory
                .select(order, orderItem, product, payment, address)
                .from(order)
                .leftJoin(order.orderItems, orderItem)
                .leftJoin(orderItem.product, product)
                .leftJoin(order.payment, payment)
                .leftJoin(order.address, address)
                .where(order.email.eq(userEmail))
                .orderBy(order.orderDate.desc(),
                        order.orderId.desc(),
                        orderItem.orderItemId.asc())
                .fetch();

        // 2) 주문 단위로 그룹핑하며 DTO를 조립한다.
        Map<Long, OrderDTO> orderMap = new LinkedHashMap<>();

        for (Tuple t : rows) {
            var o    = t.get(order);
            var oi   = t.get(orderItem);   // null 가능
            var prd  = t.get(product);     // null 가능
            var pay  = t.get(payment);     // null 가능
            var addr = t.get(address);     // null 가능

            Long oid = o.getOrderId();

            // 주문 DTO가 없다면 생성
            OrderDTO dto = orderMap.get(oid);
            if (dto == null) {
                PaymentDTO paymentDTO = (pay == null) ? null : new PaymentDTO(
                        pay.getPaymentId(),
                        pay.getPaymentMethod(),
                        pay.getPgStatus()
                );

                AddressDTO addressDTO = (addr == null) ? null : new AddressDTO(
                        addr.getAddressId(),
                        addr.getStreet(),
                        addr.getBuildingName(),
                        addr.getCity(),
                        addr.getState(),
                        addr.getCountry(),
                        addr.getPincode()
                );

                dto = new OrderDTO(
                        o.getOrderId(),
                        o.getEmail(),
                        new ArrayList<>(),      // 아이템은 아래에서 채움
                        o.getOrderDate(),
                        paymentDTO,
                        o.getTotalAmount(),
                        o.getOrderStatus(),
                        (addr != null ? addr.getAddressId() : null),
                        addressDTO
                );
                orderMap.put(oid, dto);
            }

            // 주문 아이템이 있으면 아이템 DTO 추가
            if (oi != null) {
                ProductDTO productDTO = (prd == null) ? null : new ProductDTO(
                        prd.getProductId(),
                        prd.getProductName(),
                        prd.getImage(),
                        prd.getDescription(),
                        prd.getQuantity(),
                        prd.getPrice(),
                        prd.getDiscount(),
                        prd.getSpecialPrice()
                );

                OrderItemDTO orderItemDTO = new OrderItemDTO(
                        oi.getOrderItemId(),
                        productDTO,
                        oi.getQuantity(),
                        oi.getDiscount(),
                        oi.getOrderedProductPrice()
                );

                dto.getOrderItems().add(orderItemDTO);
            }
        }

        // 3) 최신 주문 순으로 반환
        return new ArrayList<>(orderMap.values());
    }

}