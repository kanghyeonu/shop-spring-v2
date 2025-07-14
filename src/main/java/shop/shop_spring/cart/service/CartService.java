package shop.shop_spring.cart.service;

import shop.shop_spring.cart.domain.Cart;
import shop.shop_spring.cart.dto.CartDto;

public interface CartService {
    CartDto getCartForMember(Long memberId);

    void addItemToCart(Long memberId, Long productId, int quantity);

    void updateItemQuantity(Long memberId, Long cartItemId, int newQuantity);

    void removeItemFromCart(Long memberId, Long cartItemId);

    boolean clearCart(Long memberId);

    Cart getCartEntityWithItemsAndProducts(Long memberId);

}
