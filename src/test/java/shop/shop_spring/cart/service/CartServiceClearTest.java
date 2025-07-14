package shop.shop_spring.cart.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.shop_spring.cart.domain.Cart;
import shop.shop_spring.cart.domain.CartItem;
import shop.shop_spring.cart.repository.CartItemRepository;
import shop.shop_spring.cart.repository.CartRepository;
import shop.shop_spring.member.domain.Member;
import shop.shop_spring.member.service.MemberServiceImpl;
import shop.shop_spring.product.domain.Product;
import shop.shop_spring.product.service.ProductService;
import static org.assertj.core.api.Assertions.assertThat;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows; // assertThrows 임포트

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class CartServiceClearTest {

    @InjectMocks
    private CartServiceImpl cartService;

    @Mock
    private MemberServiceImpl memberService;

    @Mock
    private ProductService productService;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    private Member testMember;
    private Product testProduct;
    private CartItem existingCartItem;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .name("테스트 회원")
                .build();

        testProduct = Product.builder()
                .id(10L)
                .title("테스트 상품")
                .price(BigDecimal.valueOf(1000))
                .build();

        existingCartItem = CartItem.builder()
                .id(200L)
                .product(testProduct)
                .quantity(2)
                .build();
    }

    @Test
    void 장바구니_비우기_장바구니없음(){
        // given
        Long memberId = testMember.getId();
        when(cartRepository.findByMemberIdWithItemsAndProducts(memberId)).thenReturn(Optional.empty());

        // when
        boolean result = cartService.clearCart(memberId);

        // then
        assertThat(result).isFalse();

        verify(cartRepository, times(1)).findByMemberIdWithItemsAndProducts(memberId);
        verifyNoMoreInteractions(cartRepository);
        verifyNoInteractions(memberService, productService, cartItemRepository);

    }

    @Test
    void 장바구니_비우기_장바구니_내_상품없음(){
        Long memberId = testMember.getId();

        Cart mockEmptyCart = Mockito.mock(Cart.class);
        when(mockEmptyCart.getCartItems()).thenReturn(new ArrayList<>());

        when(cartRepository.findByMemberIdWithItemsAndProducts(memberId)).thenReturn(Optional.of(mockEmptyCart));

        boolean result = cartService.clearCart(memberId);

        assertThat(result).isFalse();

        verify(cartRepository,times(1)).findByMemberIdWithItemsAndProducts(memberId);
        verify(mockEmptyCart, times(1)).getCartItems();

        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(mockEmptyCart);
        verifyNoInteractions(memberService, productService, cartItemRepository);
    }

    @Test
    void 장바구니_비우기_성공(){
        // Given: 사용자의 장바구니는 존재하고 비어있지 않음
        Long memberId = testMember.getId();

        // Mock Cart 객체 생성
        Cart mockCart = Mockito.mock(Cart.class);

        // 테스트에서 상태 변화를 확인할 실제 CartItem 리스트 생성
        List<CartItem> realCartItemsList = new ArrayList<>();
        realCartItemsList.add(existingCartItem);

        // Mock List 객체 생성
        List<CartItem> mockCartItemsList = Mockito.mock(List.class);

        // Mock Cart의 getCartItems() 호출 시 Mock List 반환하도록 스텁
        when(mockCart.getCartItems()).thenReturn(mockCartItemsList);

        // Mock List의 isEmpty() 호출 시 false 반환하도록 스텁 (서비스 로직의 isEmpty() 검사 통과)
        when(mockCartItemsList.isEmpty()).thenReturn(false);

        // Mock List의 clear() 호출 시 실제 리스트(realCartItemsList)를 비우도록 스텁
        doAnswer(invocation -> {
            realCartItemsList.clear(); // 실제 리스트의 상태를 변경
            return null; // void 메서드이므로 null 반환
        }).when(mockCartItemsList).clear();


        // cartRepository 호출 시 Mock Cart 반환하도록 모킹
        when(cartRepository.findByMemberIdWithItemsAndProducts(memberId)).thenReturn(Optional.of(mockCart));

        // When: clearCart 호출
        boolean result = cartService.clearCart(memberId);

        // Then:
        // 메서드 반환 값이 true인지 검증
        assertThat(result).isTrue();

        // Mock 객체 호출 검증
        verify(cartRepository).findByMemberIdWithItemsAndProducts(memberId); // 장바구니 조회 호출 검증

        // Mock Cart의 getCartItems()가 호출되었는지 검증
        verify(mockCart, times(2)).getCartItems();
        // Mock List의 isEmpty()가 호출되었는지 검증
        verify(mockCartItemsList).isEmpty();
        // Mock List의 clear() 메서드가 호출되었는지 검증
        verify(mockCartItemsList).clear();

        // 실제 리스트(realCartItemsList)가 실제로 비워졌는지 검증 (doAnswer에 의해 비워졌을 것임)
        assertThat(realCartItemsList).isEmpty(); // 이 검증이 통과해야 합니다.

        // 예상치 못한 다른 Mock 객체와의 상호작용이 없었음을 검증
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(mockCart);
        verifyNoMoreInteractions(mockCartItemsList); // 예상된 호출 외에 다른 호출 없음 검증
        verifyNoInteractions(memberService, productService, cartItemRepository);

    }
}
