package shop.shop_spring.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import shop.shop_spring.security.jwt.JwtAuthenticationFilter;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    String[] urlsBePermittedAll = {
            "/products/**",
            "/main",
            "/members/register",
            "/members/login",
            "/members/password-reset",
            "/members/change-password",
            "/categories/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",

            "/api/members/register",
            "/api/members/login",
            "/api/members/password-reset",
            "/api/members/change-password",
            "/api/categories/**",
            "/api/products/**",
            "/api/swagger-ui/**",
            "/api/v3/api-docs/**"
    };

    String[] urlBeAuthenticated = {
            "/members/my-page/**",
            "/cart/**",
            "/orders/**",
            "/payments/**",

            "/api/members/my-page/**",
            "/api/cart/**",
            "/api/orders/**",
            "/api/payments/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(){
        return web -> web.ignoring()
                .requestMatchers(PathRequest
                        .toStaticResources()
                        .atCommonLocations()
                );
    }

    @Bean
    public SecurityFilterChain SecurityFilterChain(HttpSecurity http) throws Exception {

        http.csrf((csrf) -> csrf.disable()); // csrf 끄기

        http.sessionManagement((session) -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // 커스텀 필터 추가
        http.addFilterBefore(jwtAuthenticationFilter, ExceptionTranslationFilter.class);

        http.authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(urlsBePermittedAll).permitAll()
                        .requestMatchers(urlBeAuthenticated).authenticated())

                .logout(logout -> logout.permitAll());

        // .formLogin(...) 은 session 방식에서 사용함 빼야함

        return http.build();
    }
}
