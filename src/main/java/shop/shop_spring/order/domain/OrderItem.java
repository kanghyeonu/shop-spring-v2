package shop.shop_spring.order.domain;

import jakarta.persistence.*;
import lombok.*;
import shop.shop_spring.product.domain.Product;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"order", "product"})
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order; // 상품의 주문 정보

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product; // 주문된 상품

    @Column(name = "order_price", nullable = false, precision = 10)
    private BigDecimal orderPrice; // 가격

    @Column(name = "count", nullable = false)
    private Integer count; // 개수

    @Column(name = "product_name_at_order", nullable = false, length = 255)
    private String productTitleAtOrder; // 주문 당시 가격
}
