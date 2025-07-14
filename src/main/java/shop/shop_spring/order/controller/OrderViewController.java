package shop.shop_spring.order.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import shop.shop_spring.order.Dto.OrderDetailDto;
import shop.shop_spring.order.Dto.OrderSummaryDto;
import shop.shop_spring.order.sevice.OrderService;
import shop.shop_spring.order.sevice.OrderServiceImpl;
import shop.shop_spring.security.model.MyUser;

import java.util.List;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderViewController {
    private final OrderServiceImpl orderService;

    @GetMapping("/my-orders")
    public String getMyOrderPage(Authentication auth, Model model){
        MyUser member = (MyUser) auth.getPrincipal();
        List<OrderSummaryDto> orderSummaries = orderService.getOrdersByMember(member.getId());

        model.addAttribute("orderSummaries", orderSummaries);
        return "members/my-page/orders";
    }

    @GetMapping("/{orderId}")
    public String getOrderDetailsPage(@PathVariable("orderId") Long orderId, Authentication auth, Model model){
        MyUser member = (MyUser) auth.getPrincipal();

        OrderDetailDto orderDetailDto = orderService.getOrderDetails(member.getId(), orderId);

        model.addAttribute("orderDetails", orderDetailDto);
        return "orders/details";

    }
}
