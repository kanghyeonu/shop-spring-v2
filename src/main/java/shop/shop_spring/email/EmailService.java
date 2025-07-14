package shop.shop_spring.email;

import jakarta.mail.MessagingException;
import shop.shop_spring.email.dto.EmailDto;

import java.io.UnsupportedEncodingException;

public interface EmailService {
    String sendMail(EmailDto dto) throws MessagingException, UnsupportedEncodingException;
}