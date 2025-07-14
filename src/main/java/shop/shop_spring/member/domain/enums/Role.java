package shop.shop_spring.member.domain.enums;

public enum Role {
    // Spring Security에서 권장하는 권한 접두사 'ROLE_'를 사용합니다.
    ROLE_USER, // 일반 사용자 권한
    ROLE_ADMIN // 관리자 권한
}