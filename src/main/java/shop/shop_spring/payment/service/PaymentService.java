package shop.shop_spring.payment.service;

import shop.shop_spring.payment.Dto.PaymentInitiationResponse;

import java.math.BigDecimal;

public interface PaymentService {
    PaymentInitiationResponse initiatePayment(
            Long orderId,
            BigDecimal amount,
            String paymentMethod,
            String successCallbackUrl,
            String failureCallbackUrl);
}
