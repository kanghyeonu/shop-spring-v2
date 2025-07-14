package shop.shop_spring.member.domain;

import jakarta.persistence.*;
import lombok.*;
import shop.shop_spring.cart.domain.Cart;
import shop.shop_spring.member.domain.enums.Role;

import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String    username;
    @Column(nullable = false)   private String                 password;
    @Column(nullable = false)   private String                 name;
    @Column(nullable = false)   private LocalDate              birthDate;
    @Column(nullable = false)   private String                 address;
    @Column(nullable = false)   private String                 addressDetail;
    @Column(nullable = true, unique = true) private String     nickname;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Cart cart;

    public void setCart(Cart cart){
        this.cart = cart;
        if(cart != null && cart.getMember() != this){
            cart.setMember(this);
        }
    }
}
