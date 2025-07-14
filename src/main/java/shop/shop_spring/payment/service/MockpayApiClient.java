package shop.shop_spring.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import shop.shop_spring.payment.Dto.PaymentInitiationResponse;

import java.util.HashMap;
import java.util.Map;

@Service
public class MockpayApiClient implements PgApiClient{

    @Value("${pg.mypay.api-key}")
    private String apiKey;

    @Value("${pg.mypay.api-secret}")
    private String apiSecret;

    @Value("${pg.mypay.initiate-url}")
    private String initiateUrl;

    @Override
    public void setCredentials(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Override
    public Map<String, Object> requestPaymentInitiation(Map<String, Object> requestData) {
        Map<String, Object> mockPgResponse = new HashMap<>();
        mockPgResponse.put("resultCode", "0000");
        mockPgResponse.put("resultMessage", "Success");
        mockPgResponse.put("pgTransactionId", "PG_TXN_" + requestData.get("orderId") + "_" + System.currentTimeMillis());
        mockPgResponse.put(
                "redirectUrl",
                "http://localhost:8080/payments/mock-redirect?orderId=" +
                        requestData.get("orderId") +
                        "&pg_token=mock_token_" + System.currentTimeMillis());

        return mockPgResponse;
    }

    @Override
    public PaymentInitiationResponse parseInitiationResponse(Map<String, Object> pgResponse) {
        boolean success = "0000".equals(pgResponse.get("resultCode"));
        String redirectUrl = (String) pgResponse.get("redirectUrl");
        String pgTransactionId = (String) pgResponse.get("pgTransactionId");
        String errorCode = success ? null : (String) pgResponse.get("resultCode");
        String errorMessage = success ? null : (String) pgResponse.get("resultMessage");

        PaymentInitiationResponse response = PaymentInitiationResponse.builder()
                .success(success)
                .redirectUrl(redirectUrl)
                .pgTransactionId(pgTransactionId)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();

        return response;
    }
}
