package shop.shop_spring.payment.service;

import shop.shop_spring.payment.Dto.PaymentInitiationResponse;

import java.util.Map;

public interface PgApiClient {
    void setCredentials(String apiKey, String apiSecret);

    Map<String, Object> requestPaymentInitiation(Map<String, Object> requestData);

    PaymentInitiationResponse parseInitiationResponse(Map<String, Object> pgResponse);
}
