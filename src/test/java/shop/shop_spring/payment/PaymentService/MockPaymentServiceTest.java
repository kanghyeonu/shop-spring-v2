package shop.shop_spring.payment.PaymentService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import shop.shop_spring.exception.PaymentInitiationException;
import shop.shop_spring.payment.Dto.PaymentInitiationResponse;
import shop.shop_spring.payment.service.PgApiClient;
import shop.shop_spring.payment.service.RealPaymentService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MockPaymentServiceTest {
    @InjectMocks
    private RealPaymentService realPaymentService;

    @Mock
    private PgApiClient pgApiClient;

    private String successCallbackUrl = "http://localhost:8080/api/payments/mock-callback/success";
    private String failureCallbackUrl = "http://localhost:8080/api/payments/mock-callback/failure";

    @BeforeEach
    void setUp(){
        ReflectionTestUtils.setField(realPaymentService, "successCallbackUrl", successCallbackUrl);
        ReflectionTestUtils.setField(realPaymentService, "failureCallbackUrl", failureCallbackUrl);
    }

    @Test
    void 상품결제요청_성공(){
        // given
        Long orderId = 1L;
        BigDecimal amount = BigDecimal.valueOf(10000);
        String paymentMethod = "가상 결제";

        Map<String, Object> mockResponseData = new HashMap<>();
         mockResponseData.put("resultCode", "0000");
         mockResponseData.put("resultMessage", "Success");
         mockResponseData.put("redirectUrl", "http://mock-pg.com/redirect");
         mockResponseData.put("pgTransactionId", "PG_TXN_123");

        PaymentInitiationResponse successResponse = PaymentInitiationResponse.builder()
                .success(true)
                .redirectUrl("http://mock-pg.com/redirect")
                .pgTransactionId("PG_TXN_123")
                .build();
        // stub
        when(pgApiClient.requestPaymentInitiation(anyMap())).thenReturn(mockResponseData);
        when(pgApiClient.parseInitiationResponse(mockResponseData)).thenReturn(successResponse);

        // when
        PaymentInitiationResponse result = realPaymentService.initiatePayment(
                orderId,
                amount,
                paymentMethod,
                successCallbackUrl,
                failureCallbackUrl);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRedirectUrl()).isEqualTo("http://mock-pg.com/redirect");
        assertThat(result.getPgTransactionId()).isEqualTo("PG_TXN_123");

        verify(pgApiClient).requestPaymentInitiation(anyMap());
        verify(pgApiClient).parseInitiationResponse(mockResponseData);
    }

    @Test
    void 상품결제요청_실패(){
        // given
        Long orderId = 10L;
        BigDecimal amount = BigDecimal.valueOf(20000);
        String paymentMethod = "가상 결제";

        Map<String, Object> mockErrorResponseData = new HashMap<>();
        mockErrorResponseData.put("resultCode", "9999");
        mockErrorResponseData.put("resultMessage", "PG System Error");

        PaymentInitiationResponse failureResponse = PaymentInitiationResponse.builder()
                .success(false)
                .errorCode("9999")
                .errorMessage("PG System Error")
                .build();

        when(pgApiClient.requestPaymentInitiation(anyMap())).thenReturn(mockErrorResponseData);
        when(pgApiClient.parseInitiationResponse(mockErrorResponseData)).thenReturn(failureResponse);

        // when & then
        assertThrows(PaymentInitiationException.class, () -> {
            realPaymentService.initiatePayment(orderId, amount, paymentMethod, successCallbackUrl, failureCallbackUrl);
        });

        verify(pgApiClient).requestPaymentInitiation(anyMap());
        verify(pgApiClient).parseInitiationResponse(mockErrorResponseData);
    }
}
