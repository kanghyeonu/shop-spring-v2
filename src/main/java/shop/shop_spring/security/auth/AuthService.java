package shop.shop_spring.security.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import shop.shop_spring.security.jwt.JwtUtil;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtUtil jwtUtil;

    public String login(String username, String password){

        // 사용자 인증 토큰 생성
        var authToken = new UsernamePasswordAuthenticationToken(username, password);

        // 인증 토큰 인증 프로세스
        // MyUserDetailsService.loadUserByUsername() 호출
        Authentication authenticatedAuth = authenticationManagerBuilder.getObject().authenticate(authToken);

        // 인증 정보 설정
        SecurityContextHolder.getContext().setAuthentication(authenticatedAuth);

        // jwt 반환
        return jwtUtil.createToken(SecurityContextHolder.getContext().getAuthentication());
    }

}
