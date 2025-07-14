package shop.shop_spring.security.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import shop.shop_spring.member.domain.enums.Role;

import java.util.Collection;

@Getter
@Setter
public class MyUser extends User {
    // 필요 정보 추가
    private Long id;
    private String name;
    private String nickname;
    private Role role;

    public MyUser(
            String username,
            String password,
            Collection<? extends GrantedAuthority> authorities){
        super(username, password, authorities);
    }
}
