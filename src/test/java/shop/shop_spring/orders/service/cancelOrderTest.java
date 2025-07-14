package shop.shop_spring.orders.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import shop.shop_spring.cart.service.CartService;
import shop.shop_spring.exception.DataNotFoundException;
import shop.shop_spring.member.domain.Member;
import shop.shop_spring.member.service.MemberServiceImpl;
import shop.shop_spring.order.domain.Delivery;
import shop.shop_spring.order.domain.Order;
import shop.shop_spring.order.domain.OrderItem;
import shop.shop_spring.order.repository.OrderRepository;
import shop.shop_spring.order.sevice.OrderServiceImpl;
import shop.shop_spring.payment.service.PaymentService;
import shop.shop_spring.product.domain.Product;
import shop.shop_spring.product.service.ProductService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class cancelOrderTest {
    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private MemberServiceImpl memberService;
    @Mock
    private ProductService productService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private CartService cartService;

    // @Value 필드들은 모든 OrderService 테스트 클래스에 필요
    private String successCallbackUrl = "http://localhost:8080/api/payments/mock-callback/success";
    private String failureCallbackUrl = "http://localhost:8080/api/payments/mock-callback/failure";

    private Member testOrderer; // 주문자
    private Member testAnotherMember; // 다른 회원 (권한 없음 테스트용)
    private Product testProduct; // 주문 상품
    private Order testOrderPending; // 취소 가능한 PENDING 상태 주문
    private Order testOrderPaid; // 취소 가능한 PAID 상태 주문
    private Order testOrderShipped; // 취소 불가능한 SHIPPED 상태 주문

    @BeforeEach
    void setUp() {
        // 각 테스트 메서드 실행 전에 모든 Mock 객체의 스텁과 호출 기록을 초기화
        Mockito.reset(orderRepository, memberService, productService, paymentService, cartService);

        // @Value 필드들을 수동으로 주입
        ReflectionTestUtils.setField(orderService, "successCallbackUrl", successCallbackUrl);
        ReflectionTestUtils.setField(orderService, "failureCallbackUrl", failureCallbackUrl);

        // 1. 주문자 및 다른 회원 생성
        testOrderer = Member.builder()
                .id(1L)
                .name("Test Orderer")
                .build();

        testAnotherMember = Member.builder()
                .id(2L)
                .name("Another Member")
                .build();

        // 2. 상품 생성 (재고 복원 테스트를 위해 stockQuantity를 변경 가능하도록 설정)
        testProduct = Product.builder()
                .id(10L)
                .title("Test Product")
                .price(BigDecimal.valueOf(10000))
                .stockQuantity(50) // 초기 재고
                .build();

        // 3. 주문 객체 생성 (다양한 상태별로)
        // PENDING 상태 주문
        testOrderPending = Order.builder()
                .id(100L)
                .orderer(testOrderer)
                .orderDate(LocalDateTime.now())
                .status(Order.OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(20000))
                .paymentMethod("Credit Card")
                .orderItems(new ArrayList<>())
                .build();
        // OrderItem과 Product 연결 (PENDING)
        OrderItem orderItemPending = OrderItem.builder()
                .id(1L)
                .order(testOrderPending)
                .product(testProduct) // 동일한 상품 객체 참조
                .orderPrice(testProduct.getPrice())
                .count(2) // 주문 수량
                .productTitleAtOrder(testProduct.getTitle())
                .build();
        testOrderPending.addOrderItem(orderItemPending);
        // Delivery 연결 (PENDING)
        Delivery deliveryPending = Delivery.builder()
                .id(1L)
                .order(testOrderPending)
                .receiverName("Receiver P")
                .address("Address P")
                .deliveryMessage("Message P")
                .status(Delivery.DeliveryStatus.READY)
                .build();
        testOrderPending.setDelivery(deliveryPending);


        // PAID 상태 주문
        testOrderPaid = Order.builder()
                .id(101L)
                .orderer(testOrderer)
                .orderDate(LocalDateTime.now())
                .status(Order.OrderStatus.PAID)
                .totalAmount(BigDecimal.valueOf(30000))
                .paymentMethod("Bank Transfer")
                .orderItems(new ArrayList<>())
                .build();
        // OrderItem과 Product 연결 (PAID)
        OrderItem orderItemPaid = OrderItem.builder()
                .id(2L)
                .order(testOrderPaid)
                .product(testProduct) // 동일한 상품 객체 참조
                .orderPrice(testProduct.getPrice())
                .count(3) // 주문 수량
                .productTitleAtOrder(testProduct.getTitle())
                .build();
        testOrderPaid.addOrderItem(orderItemPaid);
        // Delivery 연결 (PAID)
        Delivery deliveryPaid = Delivery.builder()
                .id(2L)
                .order(testOrderPaid)
                .receiverName("Receiver Pa")
                .address("Address Pa")
                .deliveryMessage("Message Pa")
                .status(Delivery.DeliveryStatus.COMPLETED) // 배송 완료 상태로 가정 (DeliveryStatus는 별개)
                .build();
        testOrderPaid.setDelivery(deliveryPaid);


        // SHIPPED 상태 주문 (취소 불가능)
        testOrderShipped = Order.builder()
                .id(102L)
                .orderer(testOrderer)
                .orderDate(LocalDateTime.now())
                .status(Order.OrderStatus.SHIPPED)
                .totalAmount(BigDecimal.valueOf(40000))
                .paymentMethod("Credit Card")
                .orderItems(new ArrayList<>())
                .build();
        // OrderItem과 Product 연결 (SHIPPED)
        OrderItem orderItemShipped = OrderItem.builder()
                .id(3L)
                .order(testOrderShipped)
                .product(testProduct) // 동일한 상품 객체 참조
                .orderPrice(testProduct.getPrice())
                .count(4) // 주문 수량
                .productTitleAtOrder(testProduct.getTitle())
                .build();
        testOrderShipped.addOrderItem(orderItemShipped);
        // Delivery 연결 (SHIPPED)
        Delivery deliveryShipped = Delivery.builder()
                .id(3L)
                .order(testOrderShipped)
                .receiverName("Receiver Sh")
                .address("Address Sh")
                .deliveryMessage("Message Sh")
                .status(Delivery.DeliveryStatus.SHIPPING)
                .build();
        testOrderShipped.setDelivery(deliveryShipped);
    }

    @Test
    void 주문취소_성공_상태_결제대기중(){
        Long memberId = testOrderer.getId();
        Long orderId = testOrderPending.getId();
        int initialStock = testProduct.getStockQuantity(); // 초기 재고: 50
        int orderedQuantity = testOrderPending.getOrderItems().get(0).getCount(); // 주문 수량: 2

        when(orderRepository.findByIdWithOrdererItemsAndProducts(orderId)).thenReturn(Optional.of(testOrderPending));

        // When
        orderService.cancelOrder(memberId, orderId);

        // Then
        // 1. 주문 상태가 CANCELED로 변경되었는지 확인
        assertThat(testOrderPending.getStatus()).isEqualTo(Order.OrderStatus.CANCELED);
        // 2. 배송 상태가 CANCELED로 변경되었는지 확인
        assertThat(testOrderPending.getDelivery().getStatus()).isEqualTo(Delivery.DeliveryStatus.CANCELED);
        // 3. 상품 재고가 복원되었는지 확인 (initialStock + orderedQuantity)
        assertThat(testProduct.getStockQuantity()).isEqualTo(initialStock + orderedQuantity); // 50 + 2 = 52

        // 4. orderRepository.save는 @Transactional에 의해 암시적으로 호출되므로 verify하지 않음
        // 5. 다른 Mock 객체들과 상호작용 없었는지 확인
        verify(orderRepository, times(1)).findByIdWithOrdererItemsAndProducts(orderId);
        verifyNoMoreInteractions(orderRepository, memberService, productService, paymentService, cartService);
    }

    @Test
    void 주문취소_성공_상태_결제완료(){
        // Given
        Long memberId = testOrderer.getId();
        Long orderId = testOrderPaid.getId();
        int initialStock = testProduct.getStockQuantity(); // 초기 재고: 50
        int orderedQuantity = testOrderPaid.getOrderItems().get(0).getCount(); // 주문 수량: 3

        when(orderRepository.findByIdWithOrdererItemsAndProducts(orderId)).thenReturn(Optional.of(testOrderPaid));

        // when
        orderService.cancelOrder(memberId, orderId);;

        // then
        assertThat(testOrderPaid.getStatus()).isEqualTo(Order.OrderStatus.CANCELED);
        assertThat(testOrderPaid.getDelivery().getStatus()).isEqualTo(Delivery.DeliveryStatus.CANCELED);
        // 3. 상품 재고가 복원되었는지 확인 (initialStock + orderedQuantity)
        assertThat(testProduct.getStockQuantity()).isEqualTo(initialStock + orderedQuantity); // 50 + 3 = 53

        verify(orderRepository, times(1)).findByIdWithOrdererItemsAndProducts(orderId);
        verifyNoMoreInteractions(orderRepository, memberService, productService, paymentService, cartService);
    }
    
    @Test
    void 주문취소_실패_주문없음(){
        // Given
        Long memberId = testOrderer.getId();
        Long nonExistOrderId = 1000L;

        when(orderRepository.findByIdWithOrdererItemsAndProducts(nonExistOrderId)).thenReturn(Optional.empty());

        // when & then
        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, () -> {
            orderService.cancelOrder(memberId, nonExistOrderId);
        });

        assertThat(thrown.getMessage()).isEqualTo("주문을 찾을 수 없음");

        verify(orderRepository, times(1)).findByIdWithOrdererItemsAndProducts(nonExistOrderId);
        verifyNoMoreInteractions(orderRepository, memberService, productService, paymentService, cartService);
    }

    @Test
    void 주문취소_실패_권한없음(){
        // Given
        Long memberId = testAnotherMember.getId(); // <-- 다른 회원의 ID로 취소 시도
        Long orderId = testOrderPending.getId(); // <-- testOrderer가 주문한 주문 ID

        when(orderRepository.findByIdWithOrdererItemsAndProducts(orderId)).thenReturn(Optional.of(testOrderPending));

        // When & Then
        // orderService.cancelOrder 호출 시 CustomAccessDeniedException이 발생하는지 검증
        AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
            orderService.cancelOrder(memberId, orderId);
        });

        // 예외 메시지 검증
        assertThat(thrown.getMessage()).isEqualTo("접근 권한 없음");

        // Verify: orderRepository.findByIdWithOrdererItemsAndProducts는 호출되었는지 확인
        verify(orderRepository, times(1)).findByIdWithOrdererItemsAndProducts(orderId);
        // 주문 상태 변경, 재고 복원, save 등은 호출되지 않았는지 확인
        // (orderRepository.save는 @Transactional에 의해 암시적으로 호출되므로 verify하지 않음)
        verifyNoMoreInteractions(orderRepository, memberService, productService, paymentService, cartService);
    }

}
