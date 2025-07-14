package shop.shop_spring.product.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreationRequest {
    private String title;
    private BigDecimal price;
    private String username;
    private Integer stockQuantity;
    private String thumbnailUrl;
    private String description;
    private Long categoryId;
}