package shop.shop_spring.email;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@Getter
@ConfigurationProperties(prefix = "mail")
public class EmailProperties {
    private final String host;
    private final String address;
    private final String personal;
    private final String userName;
    private final String password;
    private final int port;

    @ConstructorBinding
    public EmailProperties(String host, String address, String personal, String username, String password, int port) {
        this.host = host;
        this.address = address;
        this.personal = personal;
        this.userName = username;
        this.password = password;
        this.port = port;
    }
}