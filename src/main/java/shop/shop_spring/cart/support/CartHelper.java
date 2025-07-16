package shop.shop_spring.cart.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import shop.shop_spring.cart.domain.Cart;
import shop.shop_spring.cart.domain.CartItem;
import shop.shop_spring.cart.repository.CartItemRepository;
import shop.shop_spring.cart.repository.CartRepository;
import shop.shop_spring.common.exception.DataNotFoundException;

@Component
@RequiredArgsConstructor
public class CartHelper {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public Cart getCartOrThrow(Long memberId) {
        return cartRepository.findByMemberIdWithItemsAndProducts(memberId)
                .orElseThrow(() -> new DataNotFoundException("장바구니가 비었음"));
    }

    public CartItem getCartItemOrThrow(Long cartItemId) {
        return cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new DataNotFoundException("장바구니 내 없는 상품"));
    }
}
