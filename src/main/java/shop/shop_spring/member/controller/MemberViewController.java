package shop.shop_spring.member.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import shop.shop_spring.member.domain.Member;
import shop.shop_spring.member.service.MemberServiceImpl;
import shop.shop_spring.product.Dto.ProductSearchCondition;
import shop.shop_spring.product.domain.Product;
import shop.shop_spring.product.service.ProductService;
import shop.shop_spring.security.model.MyUser;

import java.util.List;

@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberViewController {
    private final MemberServiceImpl memberService;
    private final ProductService productService;

    @GetMapping("/register")
    public String createForm(){
        return "members/register";
    }

    @GetMapping("/login")
    public String login() {
        return "members/login";
    }

    @GetMapping("/my-page")
    public String showMyPage(Authentication auth){
        MyUser user = (MyUser) auth.getPrincipal();

        return "members/my-page/my-page";
    }

    @GetMapping("/password-reset")
    public String showPasswordReset(){
        return "members/password-reset";
    }

    @GetMapping("/my-page/profile")
    public String showProfile(Authentication auth, Model model){
        MyUser myUser = (MyUser) auth.getPrincipal();
        Member member = memberService.findByUsername(myUser.getUsername());

        model.addAttribute("username", member.getUsername());
        model.addAttribute("name", member.getName());
        model.addAttribute("birthDate", member.getBirthDate());
        model.addAttribute("address", member.getAddress());
        model.addAttribute("addressDetail", member.getAddressDetail());
        model.addAttribute("nickName", member.getNickname());

        return "members/my-page/profile";
    }

    @GetMapping("/my-page/products")
    public String showProducts(Authentication auth, Model model){
        MyUser myUser = (MyUser) auth.getPrincipal();

        ProductSearchCondition searchCondition = new ProductSearchCondition();
        searchCondition.setSellerUsername(myUser.getUsername());

        List<Product> products = productService.searchProducts(searchCondition);

        model.addAttribute("username", myUser.getUsername());
        model.addAttribute("products", products);

        return "members/my-page/products";
    }

    @GetMapping("/my-page/products/{id}")
    String showDetail(@PathVariable Long id, Model model){
        Product product = productService.findById(id);

        model.addAttribute("product", product);

        return "members/my-page/edit-product";
    }


}
