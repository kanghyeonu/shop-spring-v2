package shop.shop_spring.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import shop.shop_spring.order.sevice.OrderService;

import java.util.Map;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentCallbackController {
    private final OrderService orderService;

    @PostMapping("/mock-callback/success")
    public ResponseEntity handleMockPaymentSuccessCallback(@RequestBody Map<String, Object> callbackData){
        Long orderId;
        try {
            Object orderIdObj = callbackData.get("orderId");
            if (orderIdObj == null){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: orderId missing");
            }

            if (orderIdObj instanceof Number){
                orderId = ((Number) orderIdObj).longValue();
            } else {
                orderId = Long.valueOf(orderIdObj.toString());
            }
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: invalid data format");
        }

         try {
            orderService.handlePaymentSuccessCallback(orderId);
            return ResponseEntity.status(HttpStatus.OK).body("OK");
         } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: Internal server error");
         }
    }

    @PostMapping("/mock-callback/failure")
    public ResponseEntity handleMockPaymentFailureCallback(@RequestBody Map<String, Object> callbackData){
        Long orderId;
        try {
            Object orderIdObj = callbackData.get("orderId");
            if (orderIdObj == null){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: orderId missing");
            }

            if (orderIdObj instanceof Number){
                orderId = ((Number) orderIdObj).longValue();
            } else {
                orderId = Long.valueOf(orderIdObj.toString());
            }
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: invalid data format");
        }

        try {
            orderService.handlePaymentFailureCallback(orderId);
            return ResponseEntity.status(HttpStatus.OK).body("OK");
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: Internal server error");
        }
    }
}
