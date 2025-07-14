package shop.shop_spring.cart.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceAddTest {
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
    void setUp(){
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
                .cartItems(new ArrayList<>()) // 초기에는 비어있는 장바구니
                .build();
    }

    @Test
    void 장바구니_생성_후_상품_추가(){
        // given
        when(memberService.findById(testMember.getId())).thenReturn(testMember);
        when(productService.findById(testProduct.getId())).thenReturn(testProduct);
        when(cartRepository.findByMemberIdWithItemsAndProducts(testMember.getId())).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(cartItemRepository.findByCartIdAndProductId(testCart.getId(), testProduct.getId())).thenReturn(Optional.empty());

        int quantity = 5;
        // when
        cartService.addItemToCart(testMember.getId(), testProduct.getId(), quantity);

        // then
        verify(memberService, times(1)).findById(testMember.getId());
        verify(productService, times(1)).findById(testProduct.getId());
        verify(cartRepository, times(1)).findByMemberIdWithItemsAndProducts(testMember.getId());
        verify(cartRepository, times(1)).save(any(Cart.class));

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository, times(1)).save(cartItemCaptor.capture());

        CartItem savedCartItem = cartItemCaptor.getValue();

        assertThat(savedCartItem.getCart()).isEqualTo(testCart);
        assertThat(savedCartItem.getProduct()).isEqualTo(testProduct);
        assertThat(savedCartItem.getQuantity()).isEqualTo(quantity);
    }

    @Test
    void 기존_장바구니_상품_추가(){
        // given
        when(memberService.findById(testMember.getId())).thenReturn(testMember);
        when(productService.findById(testProduct.getId())).thenReturn(testProduct);
        when(cartRepository.findByMemberIdWithItemsAndProducts(testMember.getId()))
                .thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCartIdAndProductId(testCart.getId(), testProduct.getId()))
                .thenReturn(Optional.empty());

        int quantity = 3;

        // when
        cartService.addItemToCart(testMember.getId(), testProduct.getId(), quantity);

        // then
        verify(memberService, times(1)).findById(testMember.getId());
        verify(productService, times(1)).findById(testProduct.getId());
        verify(cartRepository, times(1)).findByMemberIdWithItemsAndProducts(testMember.getId());

        verify(cartRepository, never()).save(any(Cart.class));
        verify(cartItemRepository, times(1)).findByCartIdAndProductId(testCart.getId(), testProduct.getId());
        verify(cartItemRepository, times(1)).save(any(CartItem.class));

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository, times(1)).save(cartItemCaptor.capture());
        CartItem savedCartItem = cartItemCaptor.getValue();

        assertThat(savedCartItem.getCart()).isEqualTo(testCart);
        assertThat(savedCartItem.getProduct()).isEqualTo(testProduct);
        assertThat(savedCartItem.getQuantity()).isEqualTo(quantity);
    }

    @Test
    void 장바구니_수량_업데이트(){
        // given
        when(memberService.findById(testMember.getId())).thenReturn(testMember);
        when(productService.findById(testProduct.getId())).thenReturn(testProduct);
        when(cartRepository.findByMemberIdWithItemsAndProducts(testMember.getId()))
                .thenReturn(Optional.of(testCart));

        existingCartItem = CartItem.builder()
                .id(200L)
                .cart(testCart)
                .product(testProduct)
                .quantity(2)
                .build();
        when(cartItemRepository.findByCartIdAndProductId(testCart.getId(), testProduct.getId()))
                .thenReturn(Optional.of(existingCartItem));

        int quantity = 1000;

        // when
        cartService.addItemToCart(testMember.getId(), testProduct.getId(), quantity);

        // then
        verify(memberService, times(1)).findById(testMember.getId());
        verify(productService, times(1)).findById(testProduct.getId());
        verify(cartRepository, times(1)).findByMemberIdWithItemsAndProducts(testMember.getId());

        verify(cartRepository, never()).save(any(Cart.class));
        verify(cartItemRepository, times(1)).findByCartIdAndProductId(testCart.getId(), testProduct.getId());
        verify(cartItemRepository, times(1)).save(any(CartItem.class));

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository, times(1)).save(cartItemCaptor.capture());
        CartItem savedCartItem = cartItemCaptor.getValue();

        assertThat(savedCartItem).isEqualTo(existingCartItem);
        assertThat(savedCartItem.getQuantity()).isEqualTo(quantity);
        assertThat(existingCartItem.getQuantity()).isEqualTo(quantity);
    }
}
