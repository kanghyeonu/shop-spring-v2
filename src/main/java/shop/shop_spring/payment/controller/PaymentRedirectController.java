package shop.shop_spring.payment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/payments")
public class PaymentRedirectController {

    @GetMapping("mock-redirect")
    public String handleMockPaymentRedirect(
            @RequestParam("orderId") Long orderId,
            @RequestParam(value = "pg_token", required = false) String pg_token,
            Model model){

        model.addAttribute("orderId", orderId);
        model.addAttribute("message", "결제 결과를 선택");

        return "/payments/result";

    }

}
