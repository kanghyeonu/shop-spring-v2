package shop.shop_spring.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "이메일 인증 요청 DTO")
public class EmailVerificationRequest {

    @Schema(description = "사용자 이메일 주소", example = "user@example.com", required = true)
    private String email;
}
