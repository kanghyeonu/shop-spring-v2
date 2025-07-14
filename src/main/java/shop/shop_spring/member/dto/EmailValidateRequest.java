package shop.shop_spring.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "이메일 인증 요청 DTO")
public class EmailValidateRequest {
    @Schema(description = "사용자의 이메일 주소", example = "user@example.com", required = true)
    private String email;

    @Schema(description = "사용자가 받은 인증 코드", example = "123456", required = true)
    private String code;
}
