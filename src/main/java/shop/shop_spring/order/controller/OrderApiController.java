package shop.shop_spring.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import shop.shop_spring.common.response.CustomApiResponse;
import shop.shop_spring.order.Dto.CartItemOrderRequest;
import shop.shop_spring.order.Dto.SingleItemOrderRequest;
import shop.shop_spring.order.sevice.OrderServiceImpl;
import shop.shop_spring.payment.Dto.PaymentInitiationResponse;
import shop.shop_spring.security.model.MyUser;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "order API", description = "주문 관련 API(주문 조회, 주문 등)")
public class OrderApiController {
    private final OrderServiceImpl orderService;

    @Operation(summary = "단일 상품 구매", description = "단일 상품을 정해진 수량 만큼 구매")
    @PostMapping("/single-item/{productId}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "주문 시작 및 결제 요청 정보 생성"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(상품 재고 부족, PG사 결제 이슈)"),
    })
    public ResponseEntity placeSingleItemOrder(
            @PathVariable Long productId,
            @RequestBody SingleItemOrderRequest request,
            Authentication auth){
        MyUser member = (MyUser) auth.getPrincipal();

        PaymentInitiationResponse initiationResponse = orderService.placeOrder(
                member.getId(),
                productId,
                request.getQuantity(),
                request.getDeliveryInfo(),
                request.getPaymentMethod());

        CustomApiResponse<PaymentInitiationResponse> response = CustomApiResponse.success(
                "주문 시작 및 결제 요청 정보 생성", initiationResponse);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "장바구니 상품 구매", description = "장바구니 내의 상품들을 정해진 수량만큼 구매")
    @PostMapping("/cart-items")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "주문 시작 및 결제 요청 정보 생성"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(특정 상품 재고 부족, PG사 결제 이슈)"),
    })
    public ResponseEntity placeCartItemOrder(
            @RequestBody CartItemOrderRequest request,
            Authentication auth){

        MyUser member = (MyUser) auth.getPrincipal();

        PaymentInitiationResponse initiationResponse = orderService.placeCartOrder(
                member.getId(),
                request.getDeliveryInfo(),
                request.getPaymentMethod());

        CustomApiResponse<PaymentInitiationResponse> response = CustomApiResponse.success(
                "주문 시작 및 결제 요청 정보 생성", initiationResponse);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @Operation(summary = "주문 취소", description = "배송 되기 전의 상품들을 취소")
    @PostMapping("/{orderId}/cancel")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "취소됨"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(주문 상태가 맞지않음 or 주문 존재x)"),
    })
    public ResponseEntity cancelOrder(@PathVariable("orderId") Long orderId, Authentication auth){
        MyUser member = (MyUser) auth.getPrincipal();

        orderService.cancelOrder(member.getId(), orderId);

        CustomApiResponse<Void> response = CustomApiResponse.successNoData("취소됨");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
