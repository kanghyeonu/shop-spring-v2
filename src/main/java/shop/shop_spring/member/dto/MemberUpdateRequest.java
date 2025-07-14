package shop.shop_spring.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter @Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원 정보 수정 요청 DTO")
public class MemberUpdateRequest {

    @Schema(description = "회원 ")
    private String username;
    private String password;
    private String birthDate;
    private String address;
    private String addressDetail;
    private String nickName;
}
