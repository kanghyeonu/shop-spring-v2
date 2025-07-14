package shop.shop_spring.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.shop_spring.cart.domain.Cart;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartDto {
    private Long id;
    private List<CartItemDto> items;
    private int totalItemCount;
    private int totalProductsCount;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CartDto fromEntity(Cart cart){
        List<CartItemDto> cartItemDtos = cart.getCartItems().stream()
                .map(CartItemDto::fromEntity)
                .collect(Collectors.toList());

        int totalItems = cartItemDtos.stream()
                .mapToInt(CartItemDto::getQuantity)
                .sum();
        int totalProducts = cartItemDtos.size();

        BigDecimal calculatedTotalPrice = cartItemDtos.stream()
                .map(CartItemDto::getLineItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartDto(
                cart.getId(),
                cartItemDtos,
                totalItems,
                totalProducts,
                calculatedTotalPrice,
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }

}
