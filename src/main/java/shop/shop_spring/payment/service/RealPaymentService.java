package shop.shop_spring.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import shop.shop_spring.exception.PaymentInitiationException;
import shop.shop_spring.payment.Dto.PaymentInitiationResponse;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RealPaymentService implements PaymentService{
    private final PgApiClient pgApiClient;

    @Value("${app.payment.success-callback-url}")
    private String successCallbackUrl;
    @Value("${app.payment.failure-callback-url}")
    private String failureCallbackUrl;

    @Override
    public PaymentInitiationResponse initiatePayment(
            Long orderId,
            BigDecimal amount,
            String paymentMethod,
            String successCallbackUrl,
            String failureCallbackUrl) {

        Map<String, Object> pgRequestData = new HashMap<>();
        pgRequestData.put("orderId", orderId);
        pgRequestData.put("amount", amount.toString());
        pgRequestData.put("productName", "주문 상품");
        pgRequestData.put("callbackUrl", successCallbackUrl); // PG사가 결제 완료 후 백엔드로 보낼 URL
        pgRequestData.put("redirectUrl", failureCallbackUrl); // PG사가 결제 실패/취소 후 사용자 브라우저를 돌려보낼 URL (또는 성공/실패 분리)
        pgRequestData.put("paymentMethod", paymentMethod);

        Map<String, Object> pgResponseData = pgApiClient.requestPaymentInitiation(pgRequestData);

        PaymentInitiationResponse initiationResponse = pgApiClient.parseInitiationResponse(pgResponseData);
        if (!initiationResponse.isSuccess()){
            throw new PaymentInitiationException("PG사 결제 요청 실패");
        }

        return initiationResponse;
    }

}
