package shop.shop_spring.security.jwt;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import shop.shop_spring.member.domain.enums.Role;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import shop.shop_spring.security.model.MyUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        //요청들어올때마다 실행할 코드 정의

        // 쿠키 없을 때 예외처리
        Cookie[] cookies = request.getCookies();
        if (cookies == null){
            filterChain.doFilter(request, response);
            return;
        }

        // jwt만 가져옴
        var jwtCookie = "";
        for (int i = 0; i < cookies.length; i++){
            if (cookies[i].getName().equals("jwt")){
                jwtCookie = cookies[i].getValue();
                break;
            }
        }
        // jwt가 없을 때
        if (jwtCookie.equals("")){
            filterChain.doFilter(request, response);
            return;
        }

        // Claims: jwt Cookie 내의 jwt 문자열로부터 추출된 정보들
        Claims claim;
        try {
            claim = jwtUtil.extractToken(jwtCookie);
        } catch (Exception e){
            filterChain.doFilter(request, response);
            return;
        }
        // claim으로부터 권한 정보 추출
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(claim.get("role").toString()));

        // 추출된 claim으로
        MyUser myUser = new MyUser(claim.get("username").toString(), "none", authorities);
        Double idDouble = Double.parseDouble(claim.get("id").toString());
        Long id = (long) idDouble.doubleValue();
        myUser.setId(id);
        myUser.setName(claim.get("name").toString());
        myUser.setNickname(claim.get("nickname").toString());
        myUser.setRole(Enum.valueOf(Role.class, claim.get("role").toString()));

        var authToken = new UsernamePasswordAuthenticationToken(
                myUser, null, authorities
        );

        authToken.setDetails(new WebAuthenticationDetailsSource()
                .buildDetails(request)
        );
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }

}

