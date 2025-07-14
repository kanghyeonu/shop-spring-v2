package shop.shop_spring.order.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shop.shop_spring.order.domain.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 주문 Id로 주문 조회
     * 주문 관련 상품과 상품에 대한 상품 엔티티까지 Fetch Join하여 한 번에 로딩
     * @param orderId
     * @return
     */
    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN FETCH o.orderItems oi " +
            "JOIN FETCH oi.product p " +
            "WHERE o.id = :orderId")
    Optional<Order> findByIdWithOrderItemsAndProduct(@Param("orderId") Long orderId);

    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN FETCH o.orderer m " +           // 주문자(Member) Fetch Join
            "JOIN FETCH o.delivery d " +           // 배송 정보(Delivery) Fetch Join
            "JOIN FETCH o.orderItems oi " +        // 주문 상품(OrderItem) 컬렉션 Fetch Join
            "JOIN FETCH oi.product p " +           // 각 주문 상품의 Product Fetch Join
            "WHERE o.id = :orderId")
    Optional<Order> findByIdWithAllDetails(@Param("orderId") Long orderId);


    /**
     * 특정 주문 ID로 주문 엔티티를 조회하며,
     * 연관된 Orderer(Member)와 OrderItem 컬렉션,.
     * 그리고 각 OrderItem의 Product 엔티티를 Fetch Join으로 함께 로딩
     * 주로 주문 취소와 같은 비즈니스 로직에 사용
     *
     * @param orderId 조회할 주문 ID
     * @return Order 엔티티 (Orderer, OrderItem, Product 함께 로딩) 또는 Optional.empty()
     */
    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN FETCH o.orderer m " +           // 주문자(Member) Fetch Join
            "JOIN FETCH o.orderItems oi " +        // 주문 상품(OrderItem) 컬렉션 Fetch Join
            "JOIN FETCH oi.product p " +           // 각 주문 상품의 Product Fetch Join
            "WHERE o.id = :orderId")
    Optional<Order> findByIdWithOrdererItemsAndProducts(@Param("orderId") Long orderId);

    List<Order> findByOrdererId(Long memberId);
}
