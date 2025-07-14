package shop.shop_spring.orders.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import shop.shop_spring.cart.service.CartService;
import shop.shop_spring.exception.InsufficientStockException;
import shop.shop_spring.exception.PaymentInitiationException;
import shop.shop_spring.member.domain.Member;
import shop.shop_spring.member.service.MemberServiceImpl;
import shop.shop_spring.order.Dto.DeliveryInfo;
import shop.shop_spring.order.domain.Delivery;
import shop.shop_spring.order.domain.Order;
import shop.shop_spring.order.domain.OrderItem;
import shop.shop_spring.order.repository.OrderRepository;
import shop.shop_spring.order.sevice.OrderServiceImpl;
import shop.shop_spring.payment.Dto.PaymentInitiationResponse;
import shop.shop_spring.payment.service.PaymentService;
import shop.shop_spring.product.domain.Product;
import shop.shop_spring.product.service.ProductService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class placeOrderTest {

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

    private String successCallbackUrl = "http://localhost:8080/payments/mock-callback/success";
    private String failureCallbackUrl = "http://localhost:8080/payments/mock-callback/failure";

    private Member testMember;
    private Product testProduct;
    private DeliveryInfo testDeliveryInfo;
    private String testPaymentMethod;

    @BeforeEach
    void setUp(){
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

        testDeliveryInfo = new DeliveryInfo(
                "Test Receiver",
                "Test Address",
                "Test Address Detail",
                "Test Message"
        );

        testPaymentMethod = "가상 결제";
    }

    @Test
    void 단일_상품_주문_성공(){
        // given
        Long memberId = testMember.getId();
        Long productId = testProduct.getId();
        int quantity = 5;

        when(memberService.findById(memberId)).thenReturn(testMember);
        when(productService.findById(productId)).thenReturn(testProduct);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedOrder, "id", 100L);
            return savedOrder;
        });

        PaymentInitiationResponse mockInitiationResponse = PaymentInitiationResponse.builder()
                .success(true)
                .redirectUrl("http://mock-pg.com/redirect")
                .pgTransactionId("PG_TXN_123")
                .build();

        when(paymentService.initiatePayment(anyLong(), any(BigDecimal.class), anyString(), anyString(), anyString()))
                .thenReturn(mockInitiationResponse);

        // when
        PaymentInitiationResponse result = orderService.placeOrder(memberId, productId,quantity, testDeliveryInfo, testPaymentMethod);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRedirectUrl()).isEqualTo("http://mock-pg.com/redirect");
        assertThat(result.getPgTransactionId()).isEqualTo("PG_TXN_123");

        verify(orderRepository).save(any(Order.class));
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder).isNotNull();
        assertThat(capturedOrder.getOrderer()).isEqualTo(testMember);
        assertThat(capturedOrder.getStatus()).isEqualTo(Order.OrderStatus.PENDING);

        assertThat(capturedOrder.getOrderItems()).hasSize(1);
        OrderItem capturedOrderItem = capturedOrder.getOrderItems().get(0);
        assertThat(capturedOrderItem.getProduct()).isEqualTo(testProduct);
        assertThat(capturedOrderItem.getCount()).isEqualTo(quantity);
        assertThat(capturedOrderItem.getOrderPrice()).isEqualTo(testProduct.getPrice());
        assertThat(capturedOrderItem.getProductTitleAtOrder()).isEqualTo(testProduct.getTitle());
        assertThat(capturedOrderItem.getOrder()).isEqualTo(capturedOrder);

        Delivery capturedDelivery = capturedOrder.getDelivery();
        assertThat(capturedDelivery).isNotNull();
        assertThat(capturedDelivery.getReceiverName()).isEqualTo(testDeliveryInfo.getReceiverName());
        assertThat(capturedDelivery.getAddress()).isEqualTo(testDeliveryInfo.getAddress() + " " + testDeliveryInfo.getAddressDetail()); // 상세 주소 포함
        assertThat(capturedDelivery.getDeliveryMessage()).isEqualTo(testDeliveryInfo.getDeliveryMessage());
        assertThat(capturedDelivery.getStatus()).isEqualTo(Delivery.DeliveryStatus.READY);
        assertThat(capturedDelivery.getOrder()).isEqualTo(capturedOrder);

        verify(paymentService, times(1)).initiatePayment(
                capturedOrder.getId(),
                capturedOrder.getTotalAmount(),
                capturedOrder.getPaymentMethod(),
                successCallbackUrl,
                failureCallbackUrl
        );

        verify(memberService, times(1)).findById(memberId);
        verify(productService, times(1)).findById(productId);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void 단일_상품_구매_실패_수량부족(){
        // Given
        Long memberId = testMember.getId();
        Long productId = testProduct.getId();
        int quantity = 101; // 재고보다 많음

        // 재고가 부족한 상품 Mocking
        Product lowStockProduct = Product.builder()
                .id(productId)
                .title("Low Stock Product")
                .price(BigDecimal.valueOf(10000))
                .stockQuantity(100) // 재고 100
                .build();

        when(memberService.findById(memberId)).thenReturn(testMember);
        when(productService.findById(productId)).thenReturn(lowStockProduct);

        // When & Then
        assertThrows(InsufficientStockException.class, () -> {
            orderService.placeOrder(memberId, productId, quantity, testDeliveryInfo, testPaymentMethod);
        });

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentService, never()).initiatePayment(anyLong(), any(BigDecimal.class), anyString(), anyString(), anyString());
        verify(memberService, times(1)).findById(memberId); // 회원 조회는 호출됨
        verify(productService, times(1)).findById(productId); // 상품 조회는 호출됨
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void 단일_상품_구매_실패_PG사_실패() {
        // given
        Long memberId = testMember.getId();
        Long productId = testProduct.getId();
        int quantity = 5;

        when(memberService.findById(memberId)).thenReturn(testMember);
        when(productService.findById(productId)).thenReturn(testProduct);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedOrder, "id", 100L); // 가상의 주문 ID 설정
            return savedOrder;
        });

        when(paymentService.initiatePayment(anyLong(), any(BigDecimal.class), anyString(), anyString(), anyString()))
                .thenThrow(new PaymentInitiationException("PG사 시스템 오류 발생"));

        // When & Then
        PaymentInitiationException thrown = assertThrows(PaymentInitiationException.class, () -> {
            orderService.placeOrder(memberId, productId, quantity, testDeliveryInfo, testPaymentMethod);
        });

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(paymentService, times(1)).initiatePayment(
                anyLong(), any(BigDecimal.class), anyString(), anyString(), anyString()); // initiatePayment는 호출됨
        verify(memberService, times(1)).findById(memberId);
        verify(productService, times(1)).findById(productId);
        verifyNoMoreInteractions(cartService);
    }


}
