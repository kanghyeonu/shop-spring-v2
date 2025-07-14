package shop.shop_spring.product.Dto;

import lombok.Getter;
import lombok.Setter;
import shop.shop_spring.product.enums.Status;

import java.util.List;

@Getter
@Setter
public class ProductSearchCondition {
    private Long id;
    private String productTitle;
    private String sellerUsername;
    private Long categoryId;
    private List<Status> statuses;
}
