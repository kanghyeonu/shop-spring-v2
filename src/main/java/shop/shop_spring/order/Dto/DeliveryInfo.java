package shop.shop_spring.order.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Setter @Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "배송지 정보")
public class DeliveryInfo {
    @Schema(description = "수령인 이름", example = "홍길동", required = true)
    private String receiverName;
    @Schema(description = "배송지 주소", example = "서울특별시 강남구 테헤란로 123", required = true)
    private String address;
    @Schema(description = "상세 주소", example = "테헤란로 123", required = true)
    private String addressDetail;
    @Schema(description = "주문 요청 사항", example = "취급 주의", required = true)
    private String deliveryMessage;
}
