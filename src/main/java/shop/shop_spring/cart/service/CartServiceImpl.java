package shop.shop_spring.cart.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.shop_spring.cart.domain.Cart;
import shop.shop_spring.cart.domain.CartItem;
import shop.shop_spring.cart.dto.CartDto;
import shop.shop_spring.cart.repository.CartItemRepository;
import shop.shop_spring.cart.repository.CartRepository;
import shop.shop_spring.cart.support.CartHelper;
import shop.shop_spring.common.exception.DataNotFoundException;
import shop.shop_spring.member.domain.Member;
import shop.shop_spring.member.service.MemberService;
import shop.shop_spring.product.domain.Product;
import shop.shop_spring.product.service.ProductService;

import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService{
    private final MemberService memberService;
    private final ProductService productService;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartHelper cartHelper;


    @Transactional
    @Override
    public CartDto getCartForMember(Long memberId) {
        Cart cart = cartHelper.getCartOrThrow(memberId);
        if (cart.getCartItems().isEmpty()){ return null;}
        return CartDto.fromEntity(cart);
    }

    @Transactional
    @Override
    public void addItemToCart(Long memberId, Long productId, int quantity) {
        Member member = memberService.findById(memberId);
        Product product = productService.findById(productId);
        Cart memberCart = cartRepository.findByMemberIdWithItemsAndProducts(memberId)
                .orElseGet(() -> {
                   Cart newCart = Cart.builder()
                           .member(member)
                           .build();
                   return cartRepository.save(newCart);
                });

        cartItemRepository.findByCartIdAndProductId(memberCart.getId(), productId)
                .ifPresentOrElse(
                        cartItem -> cartItem.setQuantity(quantity),
                        () -> {
                            CartItem newItem = CartItem.builder()
                                    .cart(memberCart)
                                    .product(product)
                                    .quantity(quantity)
                                    .build();

                            memberCart.addCartItem(newItem);
                            cartItemRepository.save(newItem);
                        }
                );

    }

    @Transactional
    @Override
    public void updateItemQuantity(Long memberId, Long cartItemId, int newQuantity) {
        CartItem cartItem = cartHelper.getCartItemOrThrow(cartItemId);

        if(!cartItem.getCart().getMember().getId().equals(memberId)){
            throw new AccessDeniedException("권한 없는 사용자");
        }

        if (newQuantity <= 0) {
            throw new IllegalArgumentException("0 이하의 상품 수량");
        }
        cartItem.setQuantity(newQuantity);
    }


    @Transactional
    @Override
    public void removeItemFromCart(Long memberId, Long cartItemId) {
        Cart cart = cartHelper.getCartOrThrow(memberId);
        if (cart.getCartItems().isEmpty()){
            throw new DataNotFoundException("장바구니가 비었음");
        }
        // 카드 안의 상품 검색
        CartItem cartItemToRemove = cartHelper.getCartItemOrThrow(cartItemId);

        // 일치하는거 삭제
        cartItemToRemove.getCart().removeCartItem(cartItemToRemove);
    }

    @Transactional
    @Override
    public boolean clearCart(Long memberId) {
        Cart cart = cartHelper.getCartOrThrow(memberId);
        if (cart.getCartItems().isEmpty()){ return false;}
        cartItemRepository.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();
        return true;
    }

    @Override
    public Cart getCartEntityWithItemsAndProducts(Long memberId) {
        return cartHelper.getCartOrThrow(memberId);

    }
}
