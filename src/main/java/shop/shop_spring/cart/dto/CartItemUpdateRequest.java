package shop.shop_spring.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "장바구니에 담긴 상품의 수량을 수정할 대 사용하는 DTO")
public class CartItemUpdateRequest {
    Integer quantity;
}
