package shop.shop_spring.orders.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import shop.shop_spring.cart.domain.Cart;
import shop.shop_spring.cart.domain.CartItem;
import shop.shop_spring.cart.service.CartService;
import shop.shop_spring.common.exception.DataNotFoundException;
import shop.shop_spring.member.domain.Member;
import shop.shop_spring.member.service.MemberServiceImpl;
import shop.shop_spring.order.Dto.DeliveryInfo;
import shop.shop_spring.order.Dto.OrderSummaryDto;
import shop.shop_spring.order.domain.Order;
import shop.shop_spring.order.repository.OrderRepository;
import shop.shop_spring.order.sevice.OrderServiceImpl;
import shop.shop_spring.payment.service.PaymentService;
import shop.shop_spring.product.domain.Product;
import shop.shop_spring.product.service.ProductService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class getOrdersByMemberTest {
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

    private String successCallbackUrl = "http://localhost:8080/api/payments/mock-callback/success";
    private String failureCallbackUrl = "http://localhost:8080/api/payments/mock-callback/failure";

    private Member testMember;
    private Product testProduct1;
    private Product testProduct2;
    private DeliveryInfo testDeliveryInfo;
    private String testPaymentMethod;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        Mockito.reset(orderRepository, memberService, productService, paymentService, cartService);

        ReflectionTestUtils.setField(orderService, "successCallbackUrl", successCallbackUrl);
        ReflectionTestUtils.setField(orderService, "failureCallbackUrl", failureCallbackUrl);

        testMember = Member.builder()
                .id(1L)
                .name("Test Member")
                .build();

        testProduct1 = Product.builder()
                .id(10L)
                .title("Test Product 1")
                .price(BigDecimal.valueOf(10000))
                .stockQuantity(100)
                .build();

        testProduct2 = Product.builder()
                .id(11L)
                .title("Test Product 2")
                .price(BigDecimal.valueOf(5000))
                .stockQuantity(50)
                .build();

        testDeliveryInfo = new DeliveryInfo(
                "Test Receiver",
                "Test Address",
                "Test Address Detail",
                "Test Message"
        );

        testPaymentMethod = "Credit Card";

        testCart = Cart.builder()
                .id(100L)
                .member(testMember)
                .cartItems(new ArrayList<>())
                .build();

        CartItem cartItem1 = CartItem.builder()
                .id(1L)
                .cart(testCart)
                .product(testProduct1)
                .quantity(2)
                .build();
        ReflectionTestUtils.setField(cartItem1, "cart", testCart);
        testCart.addCartItem(cartItem1);

        CartItem cartItem2 = CartItem.builder()
                .id(2L)
                .cart(testCart)
                .product(testProduct2)
                .quantity(3)
                .build();
        ReflectionTestUtils.setField(cartItem2, "cart", testCart);
        testCart.addCartItem(cartItem2);
    }

    @Test
    void 회원id기반_주문목록조회_성공(){
        // Given
        Long memberId = testMember.getId();

        // Mock Order 엔티티 생성
        Order order1 = Order.builder()
                .id(1001L)
                .orderer(testMember)
                .orderDate(LocalDateTime.of(2023, 1, 1, 10, 0))
                .status(Order.OrderStatus.PAID)
                .totalAmount(BigDecimal.valueOf(25000))
                .paymentMethod("Card")
                .build();

        Order order2 = Order.builder()
                .id(1002L)
                .orderer(testMember)
                .orderDate(LocalDateTime.of(2023, 1, 5, 15, 30))
                .status(Order.OrderStatus.SHIPPED)
                .totalAmount(BigDecimal.valueOf(50000))
                .paymentMethod("Bank Transfer")
                .build();

        List<Order> mockOrders = Arrays.asList(order1, order2);

        // Mocking: 의존성들의 동작 설정
        when(memberService.findById(memberId)).thenReturn(testMember); // 회원 존재 확인
        when(orderRepository.findByOrdererId(memberId)).thenReturn(mockOrders);

        // When
        List<OrderSummaryDto> result = orderService.getOrdersByMember(memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2); // 두 개의 주문이 반환되었는지 확인
        // 첫 번째 주문 DTO 검증
        OrderSummaryDto dto1 = result.get(0);
        assertThat(dto1.getOrderId()).isEqualTo(order1.getId());
        assertThat(dto1.getOrderDate()).isEqualTo(order1.getOrderDate());
        assertThat(dto1.getTotalAmount()).isEqualTo(order1.getTotalAmount());
        assertThat(dto1.getStatus()).isEqualTo(order1.getStatus());

        // 두 번째 주문 DTO 검증
        OrderSummaryDto dto2 = result.get(1);
        assertThat(dto2.getOrderId()).isEqualTo(order2.getId());
        assertThat(dto2.getOrderDate()).isEqualTo(order2.getOrderDate());
        assertThat(dto2.getTotalAmount()).isEqualTo(order2.getTotalAmount());
        assertThat(dto2.getStatus()).isEqualTo(order2.getStatus());

        // Verify: 의존성 메서드 호출 확인
        verify(memberService, times(1)).findById(memberId);
        verify(orderRepository, times(1)).findByOrdererId(memberId);
        verifyNoMoreInteractions(productService, paymentService, cartService); // 다른 서비스는 호출되지 않아야 함
    }

    @Test
    void 회원id기반_주문목록조회_성공_주문없음(){
        // Given
        Long memberId = testMember.getId();

        // orderRepository.findByOrdererId가 빈 리스트를 반환하도록 스텁
        when(memberService.findById(memberId)).thenReturn(testMember); // 회원 존재 확인
        when(orderRepository.findByOrdererId(memberId)).thenReturn(new ArrayList<>()); // 빈 리스트 반환

        // When
        List<OrderSummaryDto> result = orderService.getOrdersByMember(memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty(); // 결과 리스트가 비어있는지 확인

        // Verify: 의존성 메서드 호출 확인
        verify(memberService, times(1)).findById(memberId);
        verify(orderRepository, times(1)).findByOrdererId(memberId);
        verifyNoMoreInteractions(orderRepository, memberService); // 다른 서비스는 호출되지 않아야 함
    }

    @Test
    void 회원id기반_주문목록조회_실패_회원없음(){
        // Given
        Long memberId = 999L;

        when(memberService.findById(memberId)).thenThrow(new DataNotFoundException("회원 없음"));

        // when & then
        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, () -> {
            orderService.getOrdersByMember(memberId);
        });

        assertThat(thrown.getMessage()).isEqualTo("회원 없음");

        verify(orderRepository, never()).findByOrdererId(anyLong());
        verifyNoMoreInteractions(orderRepository, memberService);;
    }
}
