package shop.shop_spring.order.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "단일 상품 주문 요청 DTO")
public class SingleItemOrderRequest {

    @Schema(description = "주문 수량", example = "2", required = true)
    private int quantity;

    @Schema(description = "배송지 정보", required = true)
    private DeliveryInfo deliveryInfo;

    @Schema(description = "결제 수단 (예: CARD, KAKAO_PAY 등)", example = "CARD", required = true)
    private String paymentMethod;
}