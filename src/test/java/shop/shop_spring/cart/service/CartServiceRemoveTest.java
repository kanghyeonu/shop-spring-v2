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
import shop.shop_spring.exception.DataNotFoundException;
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
public class CartServiceRemoveTest {

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
                .price(BigDecimal.valueOf(1000))
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
   void 장바구니_상품_삭제_실패_장바구니없음(){
        // given
        Long memberId = testMember.getId();
        Long cartItemToRemove = existingCartItem.getId();

        when(cartRepository.findByMemberIdWithItemsAndProducts(memberId)).thenReturn(Optional.empty());

        assertThrows(DataNotFoundException.class, () -> {
           cartService.removeItemFromCart(memberId, cartItemToRemove);
        });

        verify(cartRepository, times(1)).findByMemberIdWithItemsAndProducts(memberId);
        verifyNoMoreInteractions(cartRepository);
        verifyNoInteractions(memberService, productService, cartItemRepository);
   }

    @Test
    void 장바구니_상품_삭제_실패_장바구니내상품없음(){
        // given
        Long memberId = testMember.getId();
        Long cartItemToRemove = existingCartItem.getId();

        Cart emptyCart = Cart.builder()
                .id(102L)
                .member(testMember)
                .cartItems(new ArrayList<>())
                .build();

        when(cartRepository.findByMemberIdWithItemsAndProducts(memberId)).thenReturn(Optional.of(emptyCart));

        // when & then
        assertThrows(DataNotFoundException.class, () ->{
            cartService.removeItemFromCart(memberId, cartItemToRemove);
        });

        verify(cartRepository, times(1)).findByMemberIdWithItemsAndProducts(memberId);
        verify(cartItemRepository, never()).findById(anyLong());
        verifyNoMoreInteractions(cartRepository);
        verifyNoInteractions(memberService, productService);

    }

    @Test
    void 장바구니_상품_삭제_실패_상품없음(){
        // given
        Long memberId = testMember.getId();
        Long nonExistingCartItemId = 21L;

        when(cartRepository.findByMemberIdWithItemsAndProducts(memberId)).thenReturn(Optional.of(testCart));

        when(cartItemRepository.findById(nonExistingCartItemId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(DataNotFoundException.class, () -> {
            cartService.removeItemFromCart(memberId, nonExistingCartItemId);
        });

        verify(cartRepository, times(1)).findByMemberIdWithItemsAndProducts(memberId);
        verify(cartItemRepository, times(1)).findById(nonExistingCartItemId);
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(cartItemRepository);
        verifyNoInteractions(memberService, productService);
    }

    @Test
    void 장바구니_상품_삭제_성공(){
        // Given: 사용자의 장바구니와 삭제하려는 아이템이 모두 존재하고 유효
        Long memberId = testMember.getId();
        Long cartItemIdToRemove = existingCartItem.getId();

        // Mock Cart 객체 생성
        Cart mockCart = Mockito.mock(Cart.class);

        List<CartItem> cartItemsList = new ArrayList<>();
        cartItemsList.add(existingCartItem);

        // Mock Cart의 getCartItems() 호출 시 테스트 리스트 반환하도록 스텁
        when(mockCart.getCartItems()).thenReturn(cartItemsList);

        // existingCartItem의 cart 필드를 Mock Cart로 설정
        existingCartItem.setCart(mockCart);

        // Mock Cart의 removeCartItem(existingCartItem) 호출 시
        // 실제 리스트에서 아이템을 제거하고 아이템의 cart 필드를 null로 설정하도록 스텁
        doAnswer(invocation -> {
            CartItem itemToRemove = invocation.getArgument(0);
            cartItemsList.remove(itemToRemove); // 테스트 리스트에서 제거
            itemToRemove.setCart(null); // 아이템의 cart 필드 null 설정
            return null; // void 메서드이므로 null 반환
        }).when(mockCart).removeCartItem(existingCartItem);

        when(cartRepository.findByMemberIdWithItemsAndProducts(memberId)).thenReturn(Optional.of(mockCart));
        when(cartItemRepository.findById(cartItemIdToRemove)).thenReturn(Optional.of(existingCartItem));

        // When
        assertDoesNotThrow(() -> {
            cartService.removeItemFromCart(memberId, cartItemIdToRemove);
        });

        // Then:
        verify(cartRepository, times(1)).findByMemberIdWithItemsAndProducts(memberId); // 장바구니 조회 호출 검증
        verify(cartItemRepository, times(1)).findById(cartItemIdToRemove); // 아이템 조회 호출 검증

        verify(mockCart, times(1)).getCartItems();
        verify(mockCart, times(1)).removeCartItem(existingCartItem);

        // 테스트 리스트에서 아이템이 제거되었는지 검증
        assertThat(cartItemsList).doesNotContain(existingCartItem);
        assertThat(cartItemsList).isEmpty();

        // existingCartItem의 cart 필드가 null로 설정되었는지 검증
        assertThat(existingCartItem.getCart()).isNull();

        // 예상치 못한 다른 Mock 객체와의 상호작용이 없었음을 검증
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(cartItemRepository);
        verifyNoMoreInteractions(mockCart);
        verifyNoInteractions(memberService, productService);

    }
}
