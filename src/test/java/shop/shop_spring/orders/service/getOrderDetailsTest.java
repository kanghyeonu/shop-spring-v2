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
import shop.shop_spring.order.Dto.OrderDetailDto;
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
public class getOrderDetailsTest {
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

    // 테스트에 사용할 공통 데이터
    private Member testMember;
    private Product testProduct;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // 이 테스트 클래스에서 사용되는 Mock 객체만 reset
        // OrderServiceImpl의 모든 final 필드를 Mock으로 선언했으므로 모두 reset합니다.
        Mockito.reset(orderRepository, memberService, productService, paymentService, cartService);

        ReflectionTestUtils.setField(orderService, "successCallbackUrl", successCallbackUrl);
        ReflectionTestUtils.setField(orderService, "failureCallbackUrl", failureCallbackUrl);

        testMember = Member.builder()
                .id(1L)
                .name("Test Member")
                .build();

        testProduct = Product.builder()
                .id(10L)
                .title("Test Product")
                .price(BigDecimal.valueOf(10000))
                .stockQuantity(100)
                .build();

        // 테스트 주문 객체 생성 (OrderDetailDto.fromEntity에서 필요한 연관 객체 포함)
        testOrder = Order.builder()
                .id(100L)
                .orderer(testMember)
                .orderDate(LocalDateTime.now())
                .status(Order.OrderStatus.PAID)
                .totalAmount(BigDecimal.valueOf(20000))
                .paymentMethod("Credit Card")
                .orderItems(new ArrayList<>()) // 초기화
                .build();

        // OrderItem과 Product 연결 (양방향 관계 설정은 DTO 변환에 직접적 영향 없음)
        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .order(testOrder)
                .product(testProduct)
                .orderPrice(testProduct.getPrice())
                .count(2)
                .productTitleAtOrder(testProduct.getTitle())
                .build();
        testOrder.addOrderItem(orderItem); // Order 엔티티의 addOrderItem 메서드 사용

        // Delivery 연결
        Delivery delivery = Delivery.builder()
                .id(1L)
                .order(testOrder)
                .receiverName("Test Receiver")
                .address("Test Address")
                .deliveryMessage("Test Message")
                .status(Delivery.DeliveryStatus.READY)
                .build();
        testOrder.setDelivery(delivery); // Order 엔티티의 setDelivery 메서드 사용
    }

    @Test
    void 주문상세조회_성공(){
        // Given
        Long memberId = testMember.getId();
        Long orderId = testOrder.getId();

        when(orderRepository.findByIdWithAllDetails(orderId)).thenReturn(Optional.of(testOrder));

        // When
        OrderDetailDto result = orderService.getOrderDetails(memberId, orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getOrdererName()).isEqualTo(testMember.getName());
        assertThat(result.getTotalAmount()).isEqualTo(testOrder.getTotalAmount());
        assertThat(result.getStatus()).isEqualTo(testOrder.getStatus());
        assertThat(result.getPaymentMethod()).isEqualTo(testOrder.getPaymentMethod());

        // 배송 정보 검증
        assertThat(result.getReceiverName()).isEqualTo(testOrder.getDelivery().getReceiverName());
        assertThat(result.getAddress()).isEqualTo(testOrder.getDelivery().getAddress());
        assertThat(result.getDeliveryMessage()).isEqualTo(testOrder.getDelivery().getDeliveryMessage());
        assertThat(result.getDeliveryStatus()).isEqualTo(testOrder.getDelivery().getStatus());

        // 주문 상품 목록 검증
        assertThat(result.getOrderItems()).hasSize(1);
        assertThat(result.getOrderItems().get(0).getProductId()).isEqualTo(testProduct.getId());
        assertThat(result.getOrderItems().get(0).getProductName()).isEqualTo(testProduct.getTitle());
        assertThat(result.getOrderItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(result.getOrderItems().get(0).getOrderPrice()).isEqualTo(testProduct.getPrice());
        assertThat(result.getOrderItems().get(0).getTotalPrice()).isEqualTo(testProduct.getPrice().multiply(BigDecimal.valueOf(2)));

        // Verify: 의존성 메서드 호출 확인
        verify(orderRepository, times(1)).findByIdWithAllDetails(orderId);
        verifyNoMoreInteractions(orderRepository, memberService, productService, paymentService, cartService); // 다른 Mock 객체들과 상호작용 없었는지 확인
    }

    @Test
    void 주문상세조회_실패_주문없음(){
        // Given
        Long memberId = testMember.getId();
        Long nonExistentOrderId = 999L;

        when(orderRepository.findByIdWithAllDetails(nonExistentOrderId)).thenReturn(Optional.empty());

        // When & Then
        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, () -> {
            orderService.getOrderDetails(memberId, nonExistentOrderId);
        });

        assertThat(thrown.getMessage()).isEqualTo("주문을 찾을 수 없음");

        verify(orderRepository, times(1)).findByIdWithAllDetails(nonExistentOrderId);
        verifyNoMoreInteractions(orderRepository, memberService, productService, paymentService);
    }

    @Test
    void 주문상세조회_실패_접근권한없음(){
        // Given
        Long anotherMemberId = 999L; // 다른 회원의 ID로 조회 시도
        Long orderId = testOrder.getId(); // testMember가 주문한 주문

        // orderRepository.findByIdWithAllDetails가 testOrder를 반환하도록 스텁 (주문은 존재함)
        when(orderRepository.findByIdWithAllDetails(orderId)).thenReturn(Optional.of(testOrder));

        // When & Then
        AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
            orderService.getOrderDetails(anotherMemberId, orderId);
        });

        // 예외 메시지 검증
        assertThat(thrown.getMessage()).isEqualTo("접근 권한 없음");

        // Verify: orderRepository.findByIdWithAllDetails는 호출되었는지 확인
        verify(orderRepository, times(1)).findByIdWithAllDetails(orderId);
        verifyNoMoreInteractions(orderRepository, memberService, productService, paymentService, cartService); // 다른 Mock 객체들과 상호작용 없었는지 확인

    }
}
