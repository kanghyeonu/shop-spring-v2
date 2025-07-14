package shop.shop_spring.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "비밀번호 초기화 요청 DTO")
public class PasswordResetRequest {
    @Schema(description = "사용자 이메일 또는 아이디", example = "user@example.com", required = true)
    private String username;

    @Schema(description = "사용자 이름", example = "홍길동", required = true)
    private String name;

    @Schema(description = "생년월일 (YYYYMMDD 형식)", example = "19900101", required = true)
    private String birthDate;
}
