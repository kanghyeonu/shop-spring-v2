package shop.shop_spring.product.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ProductDescription {
    @Id @Column(name="product_id")
    private Long id; // 아래의 MapsId에서 매핑되는 상품의 id를 가져옴

    @OneToOne
    @JoinColumn(name = "product_id")
    @MapsId
    private Product product;

    @Column(columnDefinition = "TEXT")
    private String description;
}
