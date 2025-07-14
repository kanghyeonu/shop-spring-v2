package shop.shop_spring.member.service;

import jakarta.mail.MessagingException;
import shop.shop_spring.member.dto.MemberCreationRequest;
import shop.shop_spring.member.domain.Member;

import java.io.UnsupportedEncodingException;

public interface MemberService {
    Long createMember(MemberCreationRequest request);

    void sendAuthenticationCode(String email) throws MessagingException, UnsupportedEncodingException;

    void validateAuthenticationCode(String email, String code);

    Member findById(Long memberId);

    Member findByUsername(String username);

    void updateMember(Member member);

    void validateUserInformation(String username, String name, String birthDate);

    void updatePassword(String username, String newPassword);
}
