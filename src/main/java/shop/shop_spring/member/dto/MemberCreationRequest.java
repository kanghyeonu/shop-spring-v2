package shop.shop_spring.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberCreationRequest {
    private String username;
    private String password;
    private String name;
    private String birthDate;
    private String address;
    private String addressDetail;
    private String nickname;
}
