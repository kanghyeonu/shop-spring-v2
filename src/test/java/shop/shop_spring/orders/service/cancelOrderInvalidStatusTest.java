package shop.shop_spring.orders.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import shop.shop_spring.cart.service.CartService;
import shop.shop_spring.exception.InvalidOrderStatusException;
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
public class cancelOrderInvalidStatusTest {
    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;
    @Mock private MemberServiceImpl memberService;
    @Mock private ProductService productService;
    @Mock private PaymentService paymentService;
    @Mock private CartService cartService;

    private String successCallbackUrl = "http://localhost:8080/api/payments/mock-callback/success";
    private String failureCallbackUrl = "http://localhost:8080/api/payments/mock-callback/failure";

    // 테스트에 사용할 공통 데이터
    private Member testOrderer; // 주문자
    private Product testProduct; // 주문 상품
    private Order testOrderShipped; // 배송 중 상태 주문 (취소 불가능)
    private Order testOrderDelivered; // 배송 완료 상태 주문 (취소 불가능)
    private Order testOrderCanceled; // 이미 취소된 상태 주문 (취소 불가능)

    @BeforeEach
    void setUp() {
        // 각 테스트 메서드 실행 전에 모든 Mock 객체의 스텁과 호출 기록을 초기화
        Mockito.reset(orderRepository, memberService, productService, paymentService, cartService);

        // @Value 필드들을 수동으로 주입
        ReflectionTestUtils.setField(orderService, "successCallbackUrl", successCallbackUrl);
        ReflectionTestUtils.setField(orderService, "failureCallbackUrl", failureCallbackUrl);

        // 1. 주문자 생성
        testOrderer = Member.builder()
                .id(1L)
                .name("Test Orderer")
                .build();

        // 2. 상품 생성
        testProduct = Product.builder()
                .id(10L)
                .title("Test Product")
                .price(BigDecimal.valueOf(10000))
                .stockQuantity(50)
                .build();

        // 3. 주문 객체 생성 (다양한 취소 불가능 상태별로)

        // SHIPPED 상태 주문 (취소 불가능)
        testOrderShipped = Order.builder()
                .id(102L)
                .orderer(testOrderer)
                .orderDate(LocalDateTime.now())
                .status(Order.OrderStatus.SHIPPED) // <-- 취소 불가능 상태
                .totalAmount(BigDecimal.valueOf(40000))
                .paymentMethod("Credit Card")
                .orderItems(new ArrayList<>())
                .build();
        OrderItem orderItemShipped = OrderItem.builder().id(3L).order(testOrderShipped).product(testProduct).orderPrice(testProduct.getPrice()).count(4).productTitleAtOrder(testProduct.getTitle()).build();
        testOrderShipped.addOrderItem(orderItemShipped);
        Delivery deliveryShipped = Delivery.builder().id(3L).order(testOrderShipped).receiverName("Receiver Sh").address("Address Sh").deliveryMessage("Message Sh").status(Delivery.DeliveryStatus.SHIPPING).build();
        testOrderShipped.setDelivery(deliveryShipped);

        // DELIVERED 상태 주문 (취소 불가능)
        testOrderDelivered = Order.builder()
                .id(103L)
                .orderer(testOrderer)
                .orderDate(LocalDateTime.now())
                .status(Order.OrderStatus.DELIVERED) // <-- 취소 불가능 상태
                .totalAmount(BigDecimal.valueOf(50000))
                .paymentMethod("Credit Card")
                .orderItems(new ArrayList<>())
                .build();
        OrderItem orderItemDelivered = OrderItem.builder().id(4L).order(testOrderDelivered).product(testProduct).orderPrice(testProduct.getPrice()).count(5).productTitleAtOrder(testProduct.getTitle()).build();
        testOrderDelivered.addOrderItem(orderItemDelivered);
        Delivery deliveryDelivered = Delivery.builder().id(4L).order(testOrderDelivered).receiverName("Receiver De").address("Address De").deliveryMessage("Message De").status(Delivery.DeliveryStatus.COMPLETED).build();
        testOrderDelivered.setDelivery(deliveryDelivered);

        // CANCELED 상태 주문 (취소 불가능)
        testOrderCanceled = Order.builder()
                .id(104L)
                .orderer(testOrderer)
                .orderDate(LocalDateTime.now())
                .status(Order.OrderStatus.CANCELED) // <-- 취소 불가능 상태
                .totalAmount(BigDecimal.valueOf(60000))
                .paymentMethod("Credit Card")
                .orderItems(new ArrayList<>())
                .build();
        OrderItem orderItemCanceled = OrderItem.builder().id(5L).order(testOrderCanceled).product(testProduct).orderPrice(testProduct.getPrice()).count(6).productTitleAtOrder(testProduct.getTitle()).build();
        testOrderCanceled.addOrderItem(orderItemCanceled);
        Delivery deliveryCanceled = Delivery.builder().id(5L).order(testOrderCanceled).receiverName("Receiver Ca").address("Address Ca").deliveryMessage("Message Ca").status(Delivery.DeliveryStatus.CANCELED).build();
        testOrderCanceled.setDelivery(deliveryCanceled);
    }

    @Test
    void 주문취소_실패_SHIPPED(){
        // Given
        Long memberId = testOrderer.getId();
        Long orderId = testOrderShipped.getId();

        // orderRepository.findByIdWithOrdererItemsAndProducts가 SHIPPED 상태의 주문을 반환하도록 스텁
        when(orderRepository.findByIdWithOrdererItemsAndProducts(orderId)).thenReturn(Optional.of(testOrderShipped));

        // When & Then
        InvalidOrderStatusException thrown = assertThrows(InvalidOrderStatusException.class, () -> {
            orderService.cancelOrder(memberId, orderId);
        });

        // 예외 메시지 검증
        assertThat(thrown.getMessage()).isEqualTo("현재 주문 상태(" + testOrderShipped.getStatus().getDisplayName() + ") 취소 불가");

        // Verify: 주문 상태 변경 및 재고 복원 관련 메서드는 호출되지 않았는지 확인
        verify(orderRepository, times(1)).findByIdWithOrdererItemsAndProducts(orderId);
        verifyNoMoreInteractions(orderRepository, memberService, productService, paymentService, cartService);
    }

    @Test
    void 주문취소_실패_DLIEVERED() {
        // Given
        Long memberId = testOrderer.getId();
        Long orderId = testOrderDelivered.getId();

        // orderRepository.findByIdWithOrdererItemsAndProducts가 DELIVERED 상태의 주문을 반환하도록 스텁
        when(orderRepository.findByIdWithOrdererItemsAndProducts(orderId)).thenReturn(Optional.of(testOrderDelivered));

        // When & Then
        InvalidOrderStatusException thrown = assertThrows(InvalidOrderStatusException.class, () -> {
            orderService.cancelOrder(memberId, orderId);
        });

        // 예외 메시지 검증
        assertThat(thrown.getMessage()).isEqualTo("현재 주문 상태(" + testOrderDelivered.getStatus().getDisplayName() + ") 취소 불가");

        // Verify: 주문 상태 변경 및 재고 복원 관련 메서드는 호출되지 않았는지 확인
        verify(orderRepository, times(1)).findByIdWithOrdererItemsAndProducts(orderId);
        verifyNoMoreInteractions(orderRepository, memberService, productService, paymentService, cartService);
    }

    @Test
    void 주문취소_실패_CANCELD() {
        // Given
        Long memberId = testOrderer.getId();
        Long orderId = testOrderCanceled.getId();

        // orderRepository.findByIdWithOrdererItemsAndProducts가 CANCELED 상태의 주문을 반환하도록 스텁
        when(orderRepository.findByIdWithOrdererItemsAndProducts(orderId)).thenReturn(Optional.of(testOrderCanceled));

        // When & Then
        InvalidOrderStatusException thrown = assertThrows(InvalidOrderStatusException.class, () -> {
            orderService.cancelOrder(memberId, orderId);
        });

        // 예외 메시지 검증
        assertThat(thrown.getMessage()).isEqualTo("현재 주문 상태(" + testOrderCanceled.getStatus().getDisplayName() + ") 취소 불가");

        // Verify: 주문 상태 변경 및 재고 복원 관련 메서드는 호출되지 않았는지 확인
        verify(orderRepository, times(1)).findByIdWithOrdererItemsAndProducts(orderId);
        verifyNoMoreInteractions(orderRepository, memberService, productService, paymentService, cartService);
    }

}
