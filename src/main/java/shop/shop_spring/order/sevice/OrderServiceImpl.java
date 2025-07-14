package shop.shop_spring.order.sevice;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.shop_spring.cart.domain.Cart;
import shop.shop_spring.cart.domain.CartItem;
import shop.shop_spring.cart.service.CartService;
import shop.shop_spring.exception.DataNotFoundException;
import shop.shop_spring.exception.InsufficientStockException;
import shop.shop_spring.exception.InvalidOrderStatusException;
import shop.shop_spring.member.domain.Member;
import shop.shop_spring.member.service.MemberService;
import shop.shop_spring.order.Dto.DeliveryInfo;
import shop.shop_spring.order.Dto.OrderDetailDto;
import shop.shop_spring.order.Dto.OrderSummaryDto;
import shop.shop_spring.order.domain.Delivery;
import shop.shop_spring.order.domain.Order;
import shop.shop_spring.order.domain.OrderItem;
import shop.shop_spring.order.repository.OrderRepository;
import shop.shop_spring.payment.Dto.PaymentInitiationResponse;
import shop.shop_spring.payment.service.PaymentService;
import shop.shop_spring.product.domain.Product;
import shop.shop_spring.product.service.ProductService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService{
    private final OrderRepository orderRepository;
    private final MemberService memberService;
    private final ProductService productService;
    private final PaymentService paymentService;
    private final CartService cartService;

    @Value("${app.payment.success-callback-url}")
    private String successCallbackUrl;
    @Value("${app.payment.failure-callback-url}")
    private String failureCallbackUrl;

    @Transactional
    @Override
    public PaymentInitiationResponse placeOrder(Long memberId, Long productId, int quantity, DeliveryInfo deliveryInfo, String paymentMethod) {
        // 1. 주문 상품 및 회원 조회
        Member member = memberService.findById(memberId);
        Product product = productService.findById(productId);

        // 2. 재고 체크
        if (product.getStockQuantity() < quantity){
            throw new InsufficientStockException("상품 재고 부족");
        }

        // 3. 주문 상품 생성
        OrderItem orderItem = OrderItem.builder()
                .product(product)
                .orderPrice(product.getPrice())
                .count(quantity)
                .productTitleAtOrder(product.getTitle())
                .build();

        // 4. 배송 정보 생성
        Delivery delivery = Delivery.builder()
                .receiverName(deliveryInfo.getReceiverName())
                .address(deliveryInfo.getAddress() + " " + deliveryInfo.getAddressDetail())
                .deliveryMessage(deliveryInfo.getDeliveryMessage())
                .status(Delivery.DeliveryStatus.READY)
                .build();


        // 5. 주문 생성
        Order order = Order.builder()
                .orderer(member)
                .orderDate(LocalDateTime.now())
                .status(Order.OrderStatus.PENDING)
                .totalAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .orderItems(new ArrayList<>())
                .paymentMethod(paymentMethod)
                .build();

        // 연관 관계 설정
        order.addOrderItem(orderItem);
        order.setDelivery(delivery);

        // 6. 주문 저장
        Order savedOrder = orderRepository.save(order);

        // 7. 결제 시스템 결제 요청
        PaymentInitiationResponse initiationResponse = paymentService.initiatePayment(
                savedOrder.getId(),
                savedOrder.getTotalAmount(),
                savedOrder.getPaymentMethod(),
                this.successCallbackUrl,
                this.failureCallbackUrl
        );

        return initiationResponse;
    }

    @Transactional
    @Override
    public PaymentInitiationResponse placeCartOrder(Long memberId, DeliveryInfo deliveryInfo, String paymentMethod) {
        // 1. 회원 및 장바구니 조회
        Member member = memberService.findById(memberId);
        Cart cart = cartService.getCartEntityWithItemsAndProducts(memberId);

        List<CartItem> cartItems = cart.getCartItems();
        if (cartItems.isEmpty()){
            throw new IllegalArgumentException("장바구니가 비었음. 주문 상품 없음");
        }

        // 2. 장바구니 내 상품 재고 체크
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems){
            Product product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();
            if ( product.getStockQuantity() < quantity){
                throw new InsufficientStockException("상품 재고 부족: " + product.getTitle());
            }

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .orderPrice(product.getPrice())
                    .count(quantity)
                    .productTitleAtOrder(product.getTitle())
                    .build();

            orderItems.add(orderItem);

            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }

        Delivery delivery = Delivery.builder()
                .receiverName(deliveryInfo.getReceiverName())
                .address(deliveryInfo.getAddress() + " " + deliveryInfo.getAddressDetail())
                .deliveryMessage(deliveryInfo.getDeliveryMessage())
                .status(Delivery.DeliveryStatus.READY)
                .build();

        Order order = Order.builder()
                .orderer(member)
                .orderDate(LocalDateTime.now())
                .status(Order.OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .paymentMethod(paymentMethod)
                .build();

        for (OrderItem orderItem : orderItems){
            order.addOrderItem(orderItem);
        }

        order.setDelivery(delivery);

        Order savedOrder = orderRepository.save(order);

        cartService.clearCart(memberId);

        PaymentInitiationResponse initiationResponse = paymentService.initiatePayment(
                savedOrder.getId(),
                savedOrder.getTotalAmount(),
                savedOrder.getPaymentMethod(),
                this.successCallbackUrl,
                this.failureCallbackUrl
        );

        return initiationResponse;
    }

    @Transactional
    @Override
    public void cancelOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findByIdWithOrdererItemsAndProducts(orderId)
                .orElseThrow(() -> new DataNotFoundException("주문을 찾을 수 없음"));

        if (!order.getOrderer().getId().equals(memberId)) {
            throw new AccessDeniedException("접근 권한 없음");
        }

        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
            order.getStatus() == Order.OrderStatus.DELIVERED ||
            order.getStatus() == Order.OrderStatus.CANCELED){
            throw new InvalidOrderStatusException("현재 주문 상태(" + order.getStatus().getDisplayName() + ") 취소 불가");
        }

        order.setStatus(Order.OrderStatus.CANCELED);
        order.getDelivery().setStatus(Delivery.DeliveryStatus.CANCELED);

        for (OrderItem orderItem : order.getOrderItems()){
            Product product = orderItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() + orderItem.getCount());
        }

    }

    @Transactional
    @Override
    public OrderDetailDto getOrderDetails(Long memberId, Long orderId) {
        Order order = orderRepository.findByIdWithAllDetails(orderId)
                .orElseThrow(() -> new DataNotFoundException("주문을 찾을 수 없음"));

        if (!order.getOrderer().getId().equals(memberId)) {
            throw new AccessDeniedException("접근 권한 없음");
        }

        return OrderDetailDto.fromEntity(order);
    }

    @Transactional
    @Override
    public List<OrderSummaryDto> getOrdersByMember(Long memberId) {
        memberService.findById(memberId);

        List<Order> orders = orderRepository.findByOrdererId(memberId);
        if (orders.isEmpty()){
            return new ArrayList<>();
        }

        return orders.stream()
                .map(order -> OrderSummaryDto.builder()
                        .orderId(order.getId())
                        .orderDate(order.getOrderDate())
                        .totalAmount(order.getTotalAmount())
                        .status(order.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void handlePaymentSuccessCallback(Long orderId) {
        // 1. 성공 주문 조회
        Order order = orderRepository.findByIdWithOrderItemsAndProduct(orderId)
                .orElseThrow(() -> {
                    throw new DataNotFoundException("결제 성공 처리 중 주문 찾기 실패");
                });

        // 2. 주문 상태 확인(중복 처리 방지 등)
        if (order.getStatus() != Order.OrderStatus.PENDING){
            System.out.println("주문이 이미 처리 됐음 " + order.getStatus().toString());
            return;
        }

        // 3. 주문 상태 갱신
        order.setStatus(Order.OrderStatus.PAID);

        // 4. 주문 후속 처리
        for (OrderItem orderItem : order.getOrderItems()){
            Product product = orderItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() - orderItem.getCount());
        }

        orderRepository.save(order);
    }

    @Transactional
    @Override
    public void handlePaymentFailureCallback(Long orderId) {
        // 1. 성공 주문 조회
        Order order = orderRepository.findByIdWithOrderItemsAndProduct(orderId)
                .orElseThrow(() -> {
                    throw new DataNotFoundException("결제 성공 처리 중 주문 찾기 실패");
                });

        // 2. 주문 상태 확인(중복 처리 방지)
        if (order.getStatus() != Order.OrderStatus.PENDING){
            System.out.println("주문이 이미 결제 됐거나 취소 처리 됐음 " + order.getStatus().toString());
            return;
        }

        // 3. 주문 상태 갱신
        order.setStatus(Order.OrderStatus.CANCELED);
        order.getDelivery().setStatus(Delivery.DeliveryStatus.CANCELED);

        orderRepository.save(order);
    }
}
