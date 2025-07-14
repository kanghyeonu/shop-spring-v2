package shop.shop_spring.member;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MemberForm {
    private String email;
    private String password;
    private String name;
    private String birthDate;
    private String address;
    private String addressDetail;
    private String nickname;
}
