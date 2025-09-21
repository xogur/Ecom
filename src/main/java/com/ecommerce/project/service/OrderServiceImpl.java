package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.*;
import com.ecommerce.project.payload.*;
import com.ecommerce.project.repositories.*;
import com.ecommerce.project.repositories.point.PointService;
import com.ecommerce.project.util.AuthUtil;
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
    PointService pointService;

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

    @Autowired
    private AuthUtil authUtil;

    private final JPAQueryFactory queryFactory;
    private final UserRepository userRepository;



    @Override
    @Transactional
    public OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage) {
        Cart cart = cartRepository.findCartByEmail(emailId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "email", emailId);
        }

        long cartTotal = Math.round(cart.getTotalPrice());

        User user = userRepository.findByEmail(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", emailId));

        // (선택) 프리뷰로 서버에서 한번 더 계산하여 조작 방지
        long pointsToUse = 0L; // TODO: 프론트에서 넘어온 값으로 교체
        var p = pointService.preview(user.getUserId(), cartTotal, Math.max(0, pointsToUse));
        long allowedUse = p.getPointsToUse();
        long finalPay   = p.getFinalPay();
        long willEarn   = p.getWillEarn();


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
        pointService.settleAfterOrder(user, cartTotal, 0, savedOrder.getOrderId ());

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

    // Service 구현부 (변경)
    @Override
    @Transactional
    public Page<OrderDTO> getUserOrders(
            String userEmail,
            Pageable pageable,
            LocalDate startDate,     // yyyy-MM-dd 로 들어온 값(없을 수 있음)
            LocalDate endDate,       // yyyy-MM-dd 로 들어온 값(없을 수 있음)
            Integer months           // ✅ 프론트에서 개월수로 보내면 여기로 받음(예: 1,2,3,6,12)
    ) {
        // 0) 개월수 → 날짜구간으로 환산 (프론트가 months만 보낸 경우)
        if ((startDate == null && endDate == null) && months != null && months > 0) {
            endDate = LocalDate.now();
            startDate = endDate.minusMonths(months);
        }

        // 1) where 구성 (이메일 + 선택적 기간 필터)
        BooleanBuilder where = new BooleanBuilder();
        where.and(order.email.eq(userEmail));

        if (startDate != null && endDate != null) {
            // LocalDate 컬럼이면 그대로 between 사용(양끝 포함)
            where.and(order.orderDate.between(startDate, endDate));
        } else if (startDate != null) {
            where.and(order.orderDate.goe(startDate));
        } else if (endDate != null) {
            where.and(order.orderDate.loe(endDate));
        }

        // 2) 정렬 변환
        List<OrderSpecifier<?>> orderSpecifiers = toOrderSpecifiers(pageable.getSort());

        // 3) 1단계: id 페이징
        List<Long> orderIds = queryFactory
                .select(order.orderId)
                .from(order)
                .where(where)
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier<?>[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (orderIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 4) total count (NPE 방지)
        Long totalL = queryFactory
                .select(order.orderId.count())
                .from(order)
                .where(where)
                .fetchOne();
        long total = (totalL == null) ? 0L : totalL;

        // 5) 2단계: 본문 조회 (+ 아이템 정렬 고정)
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
            var oi   = t.get(orderItem);
            var prd  = t.get(product);
            var pay  = t.get(payment);
            var addr = t.get(address);

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

        // 7) orderIds 순서 유지
        Map<Long, Integer> idOrder = new HashMap<>();
        for (int i = 0; i < orderIds.size(); i++) idOrder.put(orderIds.get(i), i);

        List<OrderDTO> content = new ArrayList<>(map.values());
        content.sort(Comparator.comparingInt(d -> idOrder.getOrDefault(d.getOrderId(), Integer.MAX_VALUE)));

        return new PageImpl<>(content, pageable, total);
    }

    /** Sort → QueryDSL OrderSpecifier 변환 (화이트리스트 방식, 그대로 사용) */
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
                default            -> order.orderDate;
            };
            specs.add(s.isAscending() ? path.asc() : path.desc());
        }
        specs.add(order.orderId.desc());
        return specs;
    }


}