package shop.shop_spring.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "비밀번호 변경 요청 DTO")
public class PasswordChangeRequest {
    @Schema(description = "사용자 이메일 또는 아이디", example = "user@example.com", required = true)
    private String username;

    @Schema(description = "새 비밀번호", example = "newSecurePassword123!", required = true)
    private String newPassword;
}
