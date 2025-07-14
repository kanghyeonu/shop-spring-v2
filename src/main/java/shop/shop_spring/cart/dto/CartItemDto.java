package shop.shop_spring.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.shop_spring.cart.domain.CartItem;
import shop.shop_spring.product.domain.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private Long id;
    private Long productId;
    private String productTitle;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal lineItemTotal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CartItemDto fromEntity(CartItem cartItem){
        if (cartItem == null){
            return null;
        }

        Product product = cartItem.getProduct();
        // product가 null이거나 (관계 매핑 오류 등) 필요한 필드가 null일 수 있으므로 방어 코드 필요
        Long productId = (product != null) ? product.getId() : null;
        String productTitle = (product != null) ? product.getTitle() : "알 수 없는 상품";
        BigDecimal productPrice = (product != null && product.getPrice() != null) ? product.getPrice() : BigDecimal.ZERO;

        // 항목별 총 금액 계산
        BigDecimal lineItemTotal = productPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return new CartItemDto(
                cartItem.getId(),
                productId,
                productTitle,
                productPrice,
                cartItem.getQuantity(),
                lineItemTotal,
                cartItem.getCreatedAt(),
                cartItem.getUpdatedAt()
        );

    }
}
