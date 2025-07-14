package shop.shop_spring.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "장바구니에 상품을 추가할 때 사용하는 요청 DTO")
public class CartAddRequest {
    Long productId;
    Integer quantity;
}
