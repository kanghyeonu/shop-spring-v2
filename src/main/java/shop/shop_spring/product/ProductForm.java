package shop.shop_spring.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductForm {
    String title;
    BigDecimal price;
    String username;
    Integer stockQuantity;
    String description;
    Long categoryId;
    String thumbnailUrl;
}
