package shop.shop_spring.order.Dto;

import lombok.*;
import shop.shop_spring.order.domain.Delivery;
import shop.shop_spring.order.domain.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Setter @Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailDto {
    // 주문 기본 정보
    private Long orderId;
    private String ordererName; // 주문자 이름
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private Order.OrderStatus status;
    private String paymentMethod;

    // 배송 정보
    private String receiverName;
    private String address;
    private String deliveryMessage;
    private Delivery.DeliveryStatus deliveryStatus; // 배송 상태

    // 주문 상품 목록
    private List<OrderItemDto> orderItems;

    // Order 엔티티로부터 DTO를 생성하는 팩토리 메서드 (선택 사항)
    // 모든 연관 관계가 Fetch Join으로 로딩되었다고 가정합니다.
    public static OrderDetailDto fromEntity(Order order) {
        // OrderItemDto 리스트 생성
        List<OrderItemDto> itemDtos = order.getOrderItems().stream()
                .map(OrderItemDto::fromEntity)
                .collect(Collectors.toList());

        return OrderDetailDto.builder()
                .orderId(order.getId())
                .ordererName(order.getOrderer().getName()) // Orderer(Member) 엔티티가 로딩되어 있어야 함
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                // 배송 정보 (Delivery 엔티티가 로딩되어 있어야 함)
                .receiverName(order.getDelivery().getReceiverName())
                .address(order.getDelivery().getAddress())
                .deliveryMessage(order.getDelivery().getDeliveryMessage())
                .deliveryStatus(order.getDelivery().getStatus())
                .orderItems(itemDtos)
                .build();
    }
}
