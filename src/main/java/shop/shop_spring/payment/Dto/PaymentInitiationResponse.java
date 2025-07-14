package shop.shop_spring.payment.Dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiationResponse {

    private boolean success; // 결제 시작 요청 성공 여부

    private String redirectUrl; // 사용자에게 리다이렉션할 PG사 결제 페이지 URL

    private String pgTransactionId; // PG사에서 발급한 고유 id

    private String errorCode; // error code

    private String errorMessage; // 결제 요청 실패 시, PG 오류 메세지
}
