package shop.shop_spring.order.domain;

import jakarta.persistence.*;
import lombok.*;
import shop.shop_spring.member.domain.Member;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"orderer", "orderItems", "delivery"})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB에 위임하여 PK 자동 생성 (MySQL 기준)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderer_id", nullable = false)
    private Member orderer; // 주문한 사람

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatus status; // 주문 상태

    @Column(name = "total_amount", nullable = false, precision = 10)
    private BigDecimal totalAmount; // 주문 금액

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Delivery delivery;  // 배송 상태

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>(); // 주문한 상품들

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;   // 결제 수단

    
    public void setDelivery(Delivery delivery){
        this.delivery = delivery;
        if (delivery != null && delivery.getOrder() != this){
            delivery.setOrder(this);
        }
    }

    public void addOrderItem(OrderItem orderItem){
        orderItems.add(orderItem);
        if (orderItem.getOrder() != this){
            orderItem.setOrder(this);
        }
    }

    public enum OrderStatus{
        PENDING("결제 대기 중"),      // 대기 중
        PAID("결제 완료"),           // 결제 완료
        SHIPPED("배송 중"),          // 배송 중
        DELIVERED("배송 완료"),      // 배송 완료
        CANCELED("취소 됨");         // 취소됨
        private final String displayName;

        OrderStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

}
