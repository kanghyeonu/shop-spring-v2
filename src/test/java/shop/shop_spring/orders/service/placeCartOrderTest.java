package shop.shop_spring.orders.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import shop.shop_spring.cart.domain.Cart;
import shop.shop_spring.cart.domain.CartItem;
import shop.shop_spring.cart.service.CartService;
import shop.shop_spring.common.exception.DataNotFoundException;
import shop.shop_spring.common.exception.InsufficientStockException;
import shop.shop_spring.common.exception.PaymentInitiationException;
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
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class placeCartOrderTest {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private MemberServiceImpl memberService;
    @Mock
    private ProductService productService; // placeCartOrder에서는 직접 사용 안함 (CartItem을 통해 접근)
    @Mock
    private PaymentService paymentService;
    @Mock
    private CartService cartService; // placeCartOrder에서 핵심 의존성

    private String successCallbackUrl = "http://localhost:8080/api/payments/mock-callback/success";
    private String failureCallbackUrl = "http://localhost:8080/api/payments/mock-callback/failure";

    private Member testMember;
    private Product testProduct1;
    private Product testProduct2;
    private DeliveryInfo testDeliveryInfo;
    private String testPaymentMethod;
    private Cart testCart; // 장바구니 구매 테스트용

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

        // 장바구니 구매 테스트를 위한 Cart 설정
        testCart = Cart.builder()
                .id(100L)
                .member(testMember)
                .cartItems(new ArrayList<>()) // 초기화
                .build();

        // CartItem 추가 (양방향 관계 설정)
        CartItem cartItem1 = CartItem.builder()
                .id(1L)
                .cart(testCart)
                .product(testProduct1)
                .quantity(2)
                .build();
        ReflectionTestUtils.setField(cartItem1, "cart", testCart); // 양방향 관계 설정
        testCart.addCartItem(cartItem1); // Cart 엔티티의 addCartItem 메서드 사용

        CartItem cartItem2 = CartItem.builder()
                .id(2L)
                .cart(testCart)
                .product(testProduct2)
                .quantity(3)
                .build();
        ReflectionTestUtils.setField(cartItem2, "cart", testCart); // 양방향 관계 설정
        testCart.addCartItem(cartItem2); // Cart 엔티티의 addCartItem 메서드 사용
    }

    @Test
    void 장바구니_상품_구매_성공(){
        // given
        Long memberId = testMember.getId();

        when(memberService.findById(memberId)).thenReturn(testMember);
        when(cartService.getCartEntityWithItemsAndProducts(memberId)).thenReturn(testCart);
        when(cartService.clearCart(memberId)).thenReturn(true); // 장바구니 비우기 성공 가정

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0, Order.class);
            ReflectionTestUtils.setField(savedOrder, "id", 200L); // 가상의 주문 ID 설정
            return savedOrder;
        });

        PaymentInitiationResponse mockInitiationResponse = PaymentInitiationResponse.builder()
                .success(true)
                .redirectUrl("http://mock-pg.com/cart-redirect")
                .pgTransactionId("PG_TXN_CART_456")
                .build();
        when(paymentService.initiatePayment(anyLong(), any(BigDecimal.class), anyString(), anyString(), anyString()))
                .thenReturn(mockInitiationResponse);

        // When
        PaymentInitiationResponse result = orderService.placeCartOrder(
                memberId, testDeliveryInfo, testPaymentMethod);

        // Then
        // 1. 반환된 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRedirectUrl()).isEqualTo("http://mock-pg.com/cart-redirect");
        assertThat(result.getPgTransactionId()).isEqualTo("PG_TXN_CART_456");

        // 2. orderRepository.save가 올바른 Order 객체로 호출되었는지 검증
        verify(orderRepository, times(1)).save(any(Order.class));
        Order capturedOrder = orderCaptor.getValue(); // 캡처된 Order 객체 가져오기

        assertThat(capturedOrder).isNotNull();
        assertThat(capturedOrder.getOrderer()).isEqualTo(testMember);
        assertThat(capturedOrder.getStatus()).isEqualTo(Order.OrderStatus.PENDING);

        // 총 금액 계산 (10000 * 2 + 5000 * 3 = 20000 + 15000 = 35000)
        BigDecimal expectedTotalAmount = BigDecimal.valueOf(10000).multiply(BigDecimal.valueOf(2))
                .add(BigDecimal.valueOf(5000).multiply(BigDecimal.valueOf(3)));
        assertThat(capturedOrder.getTotalAmount()).isEqualTo(expectedTotalAmount);
        assertThat(capturedOrder.getPaymentMethod()).isEqualTo(testPaymentMethod);

        // OrderItem 검증
        assertThat(capturedOrder.getOrderItems()).hasSize(2); // 장바구니 아이템 수만큼
        // 각 OrderItem의 내용 검증 (순서는 보장되지 않을 수 있으므로 contains 등 사용)
        OrderItem capturedOrderItem1 = capturedOrder.getOrderItems().stream()
                .filter(oi -> oi.getProduct().getId().equals(testProduct1.getId())).findFirst().orElse(null);
        OrderItem capturedOrderItem2 = capturedOrder.getOrderItems().stream()
                .filter(oi -> oi.getProduct().getId().equals(testProduct2.getId())).findFirst().orElse(null);

        assertThat(capturedOrderItem1).isNotNull();
        assertThat(capturedOrderItem1.getProduct()).isEqualTo(testProduct1);
        assertThat(capturedOrderItem1.getCount()).isEqualTo(2);
        assertThat(capturedOrderItem1.getOrderPrice()).isEqualTo(testProduct1.getPrice());

        assertThat(capturedOrderItem2).isNotNull();
        assertThat(capturedOrderItem2.getProduct()).isEqualTo(testProduct2);
        assertThat(capturedOrderItem2.getCount()).isEqualTo(3);
        assertThat(capturedOrderItem2.getOrderPrice()).isEqualTo(testProduct2.getPrice());

        // Delivery 검증
        Delivery capturedDelivery = capturedOrder.getDelivery();
        assertThat(capturedDelivery).isNotNull();
        assertThat(capturedDelivery.getReceiverName()).isEqualTo(testDeliveryInfo.getReceiverName());
        assertThat(capturedDelivery.getAddress()).isEqualTo(testDeliveryInfo.getAddress() + " " + testDeliveryInfo.getAddressDetail());
        assertThat(capturedDelivery.getDeliveryMessage()).isEqualTo(testDeliveryInfo.getDeliveryMessage());
        assertThat(capturedDelivery.getStatus()).isEqualTo(Delivery.DeliveryStatus.READY);

        // 3. cartService.clearCart가 호출되었는지 검증
        verify(cartService, times(1)).clearCart(memberId);

        // 4. paymentService.initiatePayment가 올바른 파라미터로 호출되었는지 검증
        verify(paymentService, times(1)).initiatePayment(
                capturedOrder.getId(),
                capturedOrder.getTotalAmount(),
                capturedOrder.getPaymentMethod(),
                successCallbackUrl,
                failureCallbackUrl
        );

        // 5. 다른 서비스 메서드 호출 검증
        verify(memberService, times(1)).findById(memberId);
        verify(cartService, times(1)).getCartEntityWithItemsAndProducts(memberId);
        verifyNoMoreInteractions(productService); // productService는 직접 호출되지 않아야 함 (CartItem을 통해 접근)
    }

    @Test
    void 장바구니_구매_실패_장바구니없음(){
        // Given
        Long memberId = testMember.getId();

        // memberService.findById는 정상 반환
        when(memberService.findById(memberId)).thenReturn(testMember);
        // cartService.getCartEntityWithItemsAndProducts가 DataNotFoundException을 발생시키도록 스텁
        when(cartService.getCartEntityWithItemsAndProducts(memberId))
                .thenThrow(new DataNotFoundException("장바구니를 찾을 수 없습니다."));

        // When & Then
        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, () -> {
            orderService.placeCartOrder(memberId, testDeliveryInfo, testPaymentMethod);
        });

        // 예외 메시지 검증
        assertThat(thrown.getMessage()).isEqualTo("장바구니를 찾을 수 없습니다.");

        // Verify: 장바구니를 찾지 못했으므로 주문 관련 메서드들이 호출되지 않았는지 확인
        verify(memberService, times(1)).findById(memberId);
        verify(cartService, times(1)).getCartEntityWithItemsAndProducts(memberId);
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartService, never()).clearCart(anyLong());
        verify(paymentService, never()).initiatePayment(anyLong(), any(BigDecimal.class), anyString(), anyString(), anyString());
        verifyNoMoreInteractions(productService);
    }

    @Test
    void 장바구니_구매_실패_장바구니비었음(){
        // Given
        Long memberId = testMember.getId();

        Cart emptyCart = Cart.builder()
                .id(testCart.getId())
                .member(testMember)
                .cartItems(new ArrayList<>())
                .build();

        when(memberService.findById(memberId)).thenReturn(testMember);
        when(cartService.getCartEntityWithItemsAndProducts(memberId)).thenReturn(emptyCart);

        // When & Then
        // Expected: IllegalArgumentException (장바구니가 비어있을 때 발생하는 예외)
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            orderService.placeCartOrder(memberId, testDeliveryInfo, testPaymentMethod);
        });

        // 예외 메시지 검증
        assertThat(thrown.getMessage()).isEqualTo("장바구니가 비었음. 주문 상품 없음");

        // Verify: 장바구니가 비어있으므로 주문 관련 메서드들이 호출되지 않았는지 확인
        verify(memberService, times(1)).findById(memberId);
        verify(cartService, times(1)).getCartEntityWithItemsAndProducts(memberId);
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartService, never()).clearCart(anyLong());
        verify(paymentService, never()).initiatePayment(anyLong(), any(BigDecimal.class), anyString(), anyString(), anyString());
        verifyNoMoreInteractions(productService);

    }

    @Test
    void 장바구니_구매_실패_재고부족(){
        // Given
        Long memberId = testMember.getId();

        // 재고가 부족한 상황을 만들기 위한 Product Mock
        Product lowStockProduct = Product.builder()
                .id(testProduct1.getId()) // testProduct1과 동일한 ID
                .title(testProduct1.getTitle())
                .price(testProduct1.getPrice())
                .stockQuantity(1) // <-- 재고를 1로 설정 (주문 수량 2보다 적음)
                .build();

        // 재고가 부족한 상품을 포함하는 CartItem Mock
        CartItem lowStockCartItem = CartItem.builder()
                .id(testCart.getCartItems().get(0).getId())
                .cart(testCart)
                .product(lowStockProduct) // 재고 부족 상품 연결
                .quantity(2) // 주문 수량 2
                .build();
        ReflectionTestUtils.setField(lowStockCartItem, "cart", testCart);

        // 장바구니 Mock 생성
        Cart cartWithLowStock = Cart.builder()
                .id(testCart.getId())
                .member(testMember)
                .cartItems(new ArrayList<>())
                .build();
        cartWithLowStock.addCartItem(lowStockCartItem); // 재고 부족 아이템 추가
        cartWithLowStock.addCartItem(testCart.getCartItems().get(1)); // 다른 정상 아이템도 추가

        when(memberService.findById(memberId)).thenReturn(testMember);
        when(cartService.getCartEntityWithItemsAndProducts(memberId)).thenReturn(cartWithLowStock);

        // When & Then
        InsufficientStockException thrown = assertThrows(InsufficientStockException.class, () -> {
            orderService.placeCartOrder(memberId, testDeliveryInfo, testPaymentMethod);
        });

        // 예외 메시지 검증 (어떤 상품의 재고가 부족한지 메시지에 포함될 수 있음)
        assertThat(thrown.getMessage()).contains("상품 재고 부족");

        // Verify: 재고 부족으로 인해 주문 생성 및 결제 요청이 호출되지 않았는지 확인
        verify(memberService, times(1)).findById(memberId);
        verify(cartService, times(1)).getCartEntityWithItemsAndProducts(memberId);
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartService, never()).clearCart(anyLong());
        verify(paymentService, never()).initiatePayment(anyLong(), any(BigDecimal.class), anyString(), anyString(), anyString());
        verifyNoMoreInteractions(productService);
    }

    @Test
    void 장바구니_구매_실패_PG사에러(){
        // Given
        Long memberId = testMember.getId();

        when(memberService.findById(memberId)).thenReturn(testMember);
        when(cartService.getCartEntityWithItemsAndProducts(memberId)).thenReturn(testCart);
        when(cartService.clearCart(memberId)).thenReturn(true); // 장바구니 비우기 성공 가정

        // orderRepository.save가 호출될 때 어떤 Order 객체가 전달되는지 캡처
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0, Order.class);
            ReflectionTestUtils.setField(savedOrder, "id", 200L);
            return savedOrder;
        });

        // paymentService.initiatePayment가 PaymentInitiationException을 발생시키도록 Mocking
        when(paymentService.initiatePayment(anyLong(), any(BigDecimal.class), anyString(), anyString(), anyString()))
                .thenThrow(new PaymentInitiationException("PG사 시스템 오류 발생"));

        // When & Then
        PaymentInitiationException thrown = assertThrows(PaymentInitiationException.class, () -> {
            orderService.placeCartOrder(memberId, testDeliveryInfo, testPaymentMethod);
        });

        // 예외 메시지 검증 (선택 사항)
        assertThat(thrown.getMessage()).contains("PG사 시스템 오류 발생");

        // Verify:
        // 1. 회원 및 장바구니 조회 호출 확인
        verify(memberService, times(1)).findById(memberId);
        verify(cartService, times(1)).getCartEntityWithItemsAndProducts(memberId);
        // 2. 주문 저장까지는 호출되었는지 확인
        verify(orderRepository, times(1)).save(any(Order.class));
        // 3. 장바구니 비우기도 호출되었는지 확인 (주문 저장 후 호출되므로)
        verify(cartService, times(1)).clearCart(memberId);
        // 4. paymentService.initiatePayment는 예외를 던지기 위해 호출됨
        verify(paymentService, times(1)).initiatePayment(
                anyLong(), any(BigDecimal.class), anyString(), anyString(), anyString());
        // 5. 그 외 다른 서비스는 호출되지 않음
        verifyNoMoreInteractions(productService);
    }

}
