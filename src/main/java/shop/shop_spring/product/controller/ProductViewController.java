package shop.shop_spring.product.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import shop.shop_spring.product.Dto.ProductCreationRequest;
import shop.shop_spring.product.Dto.ProductSearchCondition;
import shop.shop_spring.product.ProductForm;
import shop.shop_spring.product.domain.Product;
import shop.shop_spring.product.enums.Status;
import shop.shop_spring.product.service.ProductServiceImpl;


import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductViewController {
    private final ProductServiceImpl productService;

    @GetMapping("/{id}")
    String showDetail(@PathVariable Long id, Model model, Authentication auth){
        Product product = productService.findById(id);
        model.addAttribute("product", product);

        boolean isLoggedIn = auth != null && auth.isAuthenticated();
        model.addAttribute("isLoggedIn", isLoggedIn);

        return "products/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("isAuthenticated()")
    public String createForm(Model model, Authentication auth){
        model.addAttribute("username", auth.getName());
        return "products/productForm";
    }

    @GetMapping
    public String listProducts(
        @RequestParam(value = "title", required = false) String title,
        @RequestParam(value = "category", required = false) Long categoryId,
        Model model
    ){
        ProductSearchCondition searchCondition = new ProductSearchCondition();
        searchCondition.setProductTitle(title);
        searchCondition.setCategoryId(categoryId);

        List<Status> statuses = new ArrayList<>();
        statuses.add(Status.ACTIVE);
        statuses.add(Status.SOLD_OUT);
        searchCondition.setStatuses(statuses);

        List<Product> products = productService.searchProducts(searchCondition);

        model.addAttribute("products", products);
        return "products/list";
    }


    private ProductCreationRequest formToProductCreationRequest(ProductForm form){
        ProductCreationRequest request = new ProductCreationRequest();

        request.setTitle(form.getTitle());
        request.setPrice(form.getPrice());
        request.setUsername(form.getUsername());
        request.setStockQuantity(form.getStockQuantity());
        request.setThumbnailUrl(form.getThumbnailUrl());
        request.setDescription(form.getDescription());
        request.setCategoryId(form.getCategoryId());

        return request;
    }

}
