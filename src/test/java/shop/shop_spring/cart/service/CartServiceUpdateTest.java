package shop.shop_spring.cart.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import shop.shop_spring.cart.domain.Cart;
import shop.shop_spring.cart.domain.CartItem;
import shop.shop_spring.cart.repository.CartItemRepository;
import shop.shop_spring.cart.repository.CartRepository;
import shop.shop_spring.common.exception.DataNotFoundException;
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
import java.util.Optional;


@ExtendWith(MockitoExtension.class)
public class CartServiceUpdateTest {
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
    private Cart testCart;
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
                .price(BigDecimal.valueOf(10000))
                .build();

        testCart = Cart.builder()
                .id(100L)
                .member(testMember)
                .cartItems(new ArrayList<>())
                .build();

        existingCartItem = CartItem.builder()
                .id(200L)
                .cart(testCart)
                .product(testProduct)
                .quantity(2)
                .build();

        testCart.addCartItem(existingCartItem);
    }

    @Test
    void 장바구니_수량_업데이트_실패_없는상품(){
        // given
        Long nonExistingCartItemId = 1000L;
        when(cartItemRepository.findById(nonExistingCartItemId)).thenReturn(Optional.empty());
        int newQuantity = 19;

        // when & then
        assertThrows(DataNotFoundException.class, () -> {
           cartService.updateItemQuantity(testMember.getId(), nonExistingCartItemId, newQuantity);
        });

        verify(cartItemRepository, times(1)).findById(nonExistingCartItemId);
        verifyNoMoreInteractions(cartItemRepository);
        verifyNoInteractions(memberService, productService, cartRepository);
    }

    @Test
    void 장바구니_수량_업데이트_실패_비인가사용자(){
        // given
        Long requestingMemberId = testMember.getId(); // 수량 업데이트를 시도하는 사용자 ID

        // 다른 사용자 및 해당 사용자의 장바구니 생성
        Member anotherMember = Member.builder()
                .id(2L) // 다른 ID 사용
                .name("다른 회원")
                .build();
        Cart anotherCart = Cart.builder()
                .id(101L) // 다른 ID 사용
                .member(anotherMember)
                .cartItems(new ArrayList<>())
                .build();

        // 다른 사용자의 장바구니에 속한 장바구니 아이템 생성
        CartItem cartItemOfAnotherUser = CartItem.builder()
                .id(201L) // 다른 ID 사용
                .cart(anotherCart) // 다른 장바구니에 속해 있다고 설정
                .product(testProduct)
                .quantity(5)
                .build();
        anotherCart.addCartItem(cartItemOfAnotherUser);

        Long cartItemIdToUpdate = cartItemOfAnotherUser.getId();
        int newQuantity = 1000;

        when(cartItemRepository.findById(cartItemIdToUpdate)).thenReturn(Optional.of(cartItemOfAnotherUser));

        // when & then
        assertThrows(AccessDeniedException.class, () -> {
            cartService.updateItemQuantity(requestingMemberId, cartItemIdToUpdate, newQuantity);
        });

        verify(cartItemRepository, times(1)).findById(cartItemIdToUpdate);
        verifyNoMoreInteractions(cartItemRepository);
        verifyNoInteractions(memberService, productService, cartRepository);
    }

    @Test
    void 장바구니_수량_업데이트_실패_음수(){
        // given
        Long memberId = testMember.getId();
        Long cartItemToUpdate = existingCartItem.getId();

        when(cartItemRepository.findById(cartItemToUpdate)).thenReturn(Optional.of(existingCartItem));

        int invalidQuantity = - 199;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
           cartService.updateItemQuantity(memberId, cartItemToUpdate, invalidQuantity);
        });

        verify(cartItemRepository, times(1)).findById(cartItemToUpdate);
        verifyNoMoreInteractions(cartRepository);
        verifyNoInteractions(memberService, productService, cartRepository);
    }

    @Test
    void 장바구니_수량_업데이트_성공(){
        // Given
        Long memberId = testMember.getId();
        Long cartItemIdToUpdate = existingCartItem.getId();
        int newQuantity = 5;

        when(cartItemRepository.findById(cartItemIdToUpdate)).thenReturn(Optional.of(existingCartItem));

        // When
        assertDoesNotThrow(() -> {
            cartService.updateItemQuantity(memberId, cartItemIdToUpdate, newQuantity);
        });

        // Then:
        verify(cartItemRepository, times(1)).findById(cartItemIdToUpdate);
        assertThat(existingCartItem.getQuantity()).isEqualTo(newQuantity);

        // 다른 Mock 객체들은 호출되지 않았음을 검증
        verifyNoMoreInteractions(cartItemRepository);
        verifyNoInteractions(memberService, productService, cartRepository);
    }
}
