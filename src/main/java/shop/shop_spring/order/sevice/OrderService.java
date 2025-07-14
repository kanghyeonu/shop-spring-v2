package shop.shop_spring.order.sevice;

import shop.shop_spring.order.Dto.DeliveryInfo;
import shop.shop_spring.order.Dto.OrderDetailDto;
import shop.shop_spring.order.Dto.OrderSummaryDto;
import shop.shop_spring.payment.Dto.PaymentInitiationResponse;

import java.util.List;

public interface OrderService {

    /**
     * 단일 상품 구매
     * @param memberId 주문 회원 id
     * @param productId 주문 상품 id
     * @param quantity 주문 수량
     * @param deliveryInfo 배송 정보
     * @return
     */
    PaymentInitiationResponse placeOrder(Long memberId, Long productId, int quantity, DeliveryInfo deliveryInfo, String paymentMethod);

    /**
     *  카트 내 상품 일괄 구매
     * @param memberId
     * @param deliveryInfo
     * @return
     */
    PaymentInitiationResponse placeCartOrder(Long memberId, DeliveryInfo deliveryInfo, String paymentMethod);

    /**
     * 주문 취소
     * @param memberId
     * @param orderId
     */
    void cancelOrder(Long memberId, Long orderId);

    /**
     * 주문 상세 조회
     * @param memberId
     * @param orderId
     * @return
     */
    OrderDetailDto getOrderDetails(Long memberId, Long orderId);

    /**
     * 주문 리스트 조회
     * @param memberId
     * @return
     */
    List<OrderSummaryDto> getOrdersByMember(Long memberId);

    void handlePaymentSuccessCallback(Long orderId);

    void handlePaymentFailureCallback(Long orderId);

}
