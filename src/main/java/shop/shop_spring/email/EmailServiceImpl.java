package shop.shop_spring.email;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import shop.shop_spring.email.dto.EmailDto;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService{
    @Value("${spring.mail.username}")
    private String address;
    @Value("${spring.mail.personal}")
    private String personal;

    private final JavaMailSender emailSender;

    @Override
    public String sendMail(EmailDto dto) throws MessagingException, UnsupportedEncodingException{
        MimeMessage message = createMessage(dto.getEmail(), dto.getTitle(), dto.getText());
        emailSender.send(message);
        return "SUCCESS";
    }

    private MimeMessage createMessage(String recipient, String title, String text) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = emailSender.createMimeMessage();
        message.setFrom(new InternetAddress(address, personal));
        message.setRecipients(Message.RecipientType.TO, recipient);
        message.setSubject(title);
        message.setText(text, "UTF-8", "html");
        return message;
    }
}
