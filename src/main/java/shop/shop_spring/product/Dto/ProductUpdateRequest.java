package shop.shop_spring.product.Dto;

import lombok.Getter;
import lombok.Setter;
import shop.shop_spring.product.enums.Status;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductUpdateRequest {
    private String title;
    private BigDecimal price;
    private Status status;
    private Integer stockQuantity;
    private String thumbnailUrl;
    private String description;
}
