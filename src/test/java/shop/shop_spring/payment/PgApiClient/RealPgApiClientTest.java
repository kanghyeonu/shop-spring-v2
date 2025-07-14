package shop.shop_spring.payment.PgApiClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import shop.shop_spring.payment.Dto.PaymentInitiationResponse;
import shop.shop_spring.payment.service.MockpayApiClient;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class RealPgApiClientTest {
    @InjectMocks
    private MockpayApiClient mockpayApiClient; // 테스트 대상 클라이언트

    // @Value로 주입되는 필드들을 테스트에서 설정
    private String apiKey = "test_api_key";
    private String apiSecret = "test_api_secret";
    private String initiateUrl = "http://test-pg.com/initiate";

    @BeforeEach
    void setUp(){
        ReflectionTestUtils.setField(mockpayApiClient, "apiKey", apiKey);
        ReflectionTestUtils.setField(mockpayApiClient, "apiSecret", apiSecret);
        ReflectionTestUtils.setField(mockpayApiClient, "initiateUrl", initiateUrl);
    }

    @Test
    void API키_시크릿_설정() {
        // Given
        String newApiKey = "new_api_key";
        String newApiSecret = "new_api_secret";

        // When
        mockpayApiClient.setCredentials(newApiKey, newApiSecret); // <-- 이 라인 추가

        // Then
        assertThat(ReflectionTestUtils.getField(mockpayApiClient, "apiKey")).isEqualTo(newApiKey);
        assertThat(ReflectionTestUtils.getField(mockpayApiClient, "apiSecret")).isEqualTo(newApiSecret);
    }

    @Test
    void 결제요청_PG_생성_성공(){
        // given
        Long orderId = 123L;
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("orderId", orderId);
        requestData.put("amount", "10000");
        requestData.put("productTitle", "test product");

        // when
        Map<String, Object> response = mockpayApiClient.requestPaymentInitiation(requestData);

        // then
        assertThat(response).isNotNull();
        assertThat(response.get("resultCode")).isEqualTo("0000"); // 성공 코드 확인
        assertThat(response).containsKey("redirectUrl");
        assertThat(response.get("redirectUrl").toString()).contains("http://localhost:8080/payments/mock-redirect"); // 로컬호스트 URL 확인
        assertThat(response.get("redirectUrl").toString()).contains("orderId=" + orderId); // orderId 포함 확인
        assertThat(response).containsKey("pgTransactionId");
        assertThat(response.get("pgTransactionId").toString()).startsWith("PG_TXN_" + orderId);
    }

    @Test
    void 결제_요청_성공_응답_파싱(){
        // Given
        Map<String, Object> pgResponse = new HashMap<>();
        pgResponse.put("resultCode", "0000");
        pgResponse.put("resultMessage", "Success");
        pgResponse.put("pgTransactionId", "PG_TXN_SUCCESS_456");
        pgResponse.put("redirectUrl", "http://mock-pg.com/success_redirect");

        // when
        PaymentInitiationResponse parsedResponse = mockpayApiClient.parseInitiationResponse(pgResponse);

        // then
        assertThat(parsedResponse).isNotNull();
        assertThat(parsedResponse.isSuccess()).isTrue();
        assertThat(parsedResponse.getRedirectUrl()).isEqualTo("http://mock-pg.com/success_redirect");
        assertThat(parsedResponse.getPgTransactionId()).isEqualTo("PG_TXN_SUCCESS_456");
        assertThat(parsedResponse.getErrorCode()).isNull();
        assertThat(parsedResponse.getErrorMessage()).isNull();

    }

    @Test
    void 결제_요청_실패_응답_파싱(){
        // Given
        Map<String, Object> pgResponse = new HashMap<>();
        pgResponse.put("resultCode", "9999"); // 실패 코드
        pgResponse.put("resultMessage", "Payment failed due to insufficient funds"); // 실패 메시지
        pgResponse.put("pgTransactionId", "PG_TXN_FAILURE_789");
        pgResponse.put("redirectUrl", "http://mock-pg.com/failure_redirect");

        // when
        PaymentInitiationResponse parsedResponse = mockpayApiClient.parseInitiationResponse(pgResponse);

        // Then
        assertThat(parsedResponse).isNotNull();
        assertThat(parsedResponse.isSuccess()).isFalse(); // 실패 확인
        assertThat(parsedResponse.getRedirectUrl()).isEqualTo("http://mock-pg.com/failure_redirect");
        assertThat(parsedResponse.getPgTransactionId()).isEqualTo("PG_TXN_FAILURE_789");
        assertThat(parsedResponse.getErrorCode()).isEqualTo("9999"); // 에러 코드 확인
        assertThat(parsedResponse.getErrorMessage()).isEqualTo("Payment failed due to insufficient funds"); // 에러 메시지 확인
    }
}
