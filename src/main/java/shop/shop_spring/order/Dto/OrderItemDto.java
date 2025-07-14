package shop.shop_spring.order.Dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDto {
    private Long productId; // 주문된 상품의 ID
    private String productName; // 주문 당시 상품 이름
    private int quantity; // 주문 수량
    private BigDecimal orderPrice; // 주문 당시 상품 가격 (개당)
    private BigDecimal totalPrice; // 해당 주문 상품의 총 가격 (quantity * orderPrice)

    // OrderItem 엔티티로부터 DTO를 생성하는 팩토리 메서드 (선택 사항)
    // OrderItem 엔티티는 Product 엔티티를 포함하므로, 이를 활용합니다.
    public static OrderItemDto fromEntity(shop.shop_spring.order.domain.OrderItem orderItem) {
        return OrderItemDto.builder()
                .productId(orderItem.getProduct().getId())
                .productName(orderItem.getProductTitleAtOrder())
                .quantity(orderItem.getCount())
                .orderPrice(orderItem.getOrderPrice())
                .totalPrice(orderItem.getOrderPrice().multiply(BigDecimal.valueOf(orderItem.getCount())))
                .build();
    }
}
