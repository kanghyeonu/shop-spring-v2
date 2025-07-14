package shop.shop_spring.category.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import shop.shop_spring.category.service.CategoryService;
import shop.shop_spring.category.domain.Category;

import java.util.List;

@Controller
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories API", description = "카테고리 관련 API")
public class CategoryApiController {

    private final CategoryService categoryService;

    @Operation(summary = "상품 상위 카테고리 조회", description = "최상위 카테고리를 조회")
    @GetMapping("/parents")
    @ResponseBody
    public List<Category> getParentCategories() {
        return categoryService.findByParentIsNull();
    }

    @Operation(summary = "상품 하위 카테고리 조회", description = "하위 카테고리를 조회")
    @GetMapping("/children")
    @ResponseBody
    public List<Category> getChildCategories(@RequestParam Long parentId){
        return categoryService.findByParentId(parentId);
    }
}
