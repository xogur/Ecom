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
import org.springframework.data.domain.*;
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
    public Page<OrderDTO> getUserOrders(String userEmail, Pageable pageable) {

        // 1) where
        //   (필요 시 status, 기간 필터 추가 가능)
        var where = order.email.eq(userEmail);

        // 2) 정렬 변환
        var orderSpecifiers = toOrderSpecifiers(pageable.getSort());

        // 3) 1단계: id만 정렬+limit/offset으로 페이지 조회
        List<Long> orderIds = queryFactory
                .select(order.orderId)
                .from(order)
                .where(where)
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (orderIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 4) total count
        long total = queryFactory
                .select(order.orderId.count())
                .from(order)
                .where(where)
                .fetchOne();

        // 5) 2단계: 본문 조회 (to-one + 컬렉션 조인)
        List<Tuple> rows = queryFactory
                .select(order, orderItem, product, payment, address)
                .from(order)
                .leftJoin(order.orderItems, orderItem)
                .leftJoin(orderItem.product, product)
                .leftJoin(order.payment, payment)
                .leftJoin(order.address, address)
                .where(order.orderId.in(orderIds))
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .fetch();

        // 6) 그룹핑 & DTO 조립
        Map<Long, OrderDTO> map = new LinkedHashMap<>();
        for (Tuple t : rows) {
            var o    = t.get(order);
            var oi   = t.get(orderItem);   // null 가능
            var prd  = t.get(product);     // null 가능
            var pay  = t.get(payment);     // null 가능
            var addr = t.get(address);     // null 가능

            var dto = map.get(o.getOrderId());
            if (dto == null) {
                PaymentDTO paymentDTO = (pay == null) ? null : new PaymentDTO(
                        pay.getPaymentId(), pay.getPaymentMethod(), pay.getPgStatus()
                );
                AddressDTO addressDTO = (addr == null) ? null : new AddressDTO(
                        addr.getAddressId(), addr.getStreet(), addr.getBuildingName(),
                        addr.getCity(), addr.getState(), addr.getCountry(), addr.getPincode()
                );
                dto = new OrderDTO(
                        o.getOrderId(), o.getEmail(), new ArrayList<>(),
                        o.getOrderDate(), paymentDTO, o.getTotalAmount(),
                        o.getOrderStatus(), (addr != null ? addr.getAddressId() : null), addressDTO
                );
                map.put(o.getOrderId(), dto);
            }

            if (oi != null) {
                ProductDTO productDTO = (prd == null) ? null : new ProductDTO(
                        prd.getProductId(), prd.getProductName(), prd.getImage(),
                        prd.getDescription(), prd.getQuantity(), prd.getPrice(),
                        prd.getDiscount(), prd.getSpecialPrice()
                );
                dto.getOrderItems().add(new OrderItemDTO(
                        oi.getOrderItemId(), productDTO, oi.getQuantity(),
                        oi.getDiscount(), oi.getOrderedProductPrice()
                ));
            }
        }

        // 7) orderIds 순서 유지(안정성)
        Map<Long, Integer> idOrder = new HashMap<>();
        for (int i = 0; i < orderIds.size(); i++) idOrder.put(orderIds.get(i), i);

        List<OrderDTO> content = new ArrayList<>(map.values());
        content.sort(Comparator.comparingInt(d -> idOrder.getOrDefault(d.getOrderId(), Integer.MAX_VALUE)));

        return new PageImpl<> (content, pageable, total);
    }

    /** Sort → QueryDSL OrderSpecifier 변환 (화이트리스트 방식) */
    private List<OrderSpecifier<?>> toOrderSpecifiers(Sort sort) {
        List<OrderSpecifier<?>> specs = new ArrayList<>();
        if (sort == null || sort.isUnsorted()) {
            specs.add(order.orderDate.desc());
            specs.add(order.orderId.desc());
            return specs;
        }
        for (Sort.Order s : sort) {
            ComparableExpressionBase<?> path = switch (s.getProperty()) {
                case "orderDate"   -> order.orderDate;
                case "orderId"     -> order.orderId;
                case "totalAmount" -> order.totalAmount;
                case "orderStatus" -> order.orderStatus;
                default            -> order.orderDate; // 미허용 컬럼은 기본값으로
            };
            specs.add(s.isAscending() ? path.asc() : path.desc());
        }
        // 2차 정렬로 안정성 확보
        specs.add(order.orderId.desc());
        return specs;
    }

}