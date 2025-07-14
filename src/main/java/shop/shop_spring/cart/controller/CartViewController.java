package shop.shop_spring.cart.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import shop.shop_spring.cart.dto.CartDto;
import shop.shop_spring.cart.service.CartServiceImpl;
import shop.shop_spring.security.model.MyUser;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartViewController {
    private final CartServiceImpl cartService;

    @GetMapping("/items")
    public String showCart(Authentication auth, Model model){
        MyUser member = (MyUser) auth.getPrincipal();

        CartDto cartDto = cartService.getCartForMember(member.getId());

        boolean isLoggedIn = auth != null && auth.isAuthenticated();
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("cart", cartDto);

        return "members/my-page/cartItems";
    }
}
