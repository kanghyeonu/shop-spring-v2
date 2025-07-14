package shop.shop_spring.product.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shop.shop_spring.common.response.CustomApiResponse;
import shop.shop_spring.product.Dto.ProductCreationRequest;
import shop.shop_spring.product.service.ProductServiceImpl;


import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductApiController {
    private final ProductServiceImpl productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CustomApiResponse<Map<String, String>>> createProduct(@RequestBody ProductCreationRequest request){
        Long Id = productService.createProduct(request);

        Map<String, String> responseData = CustomApiResponse.createResponseData("상품 아이디", Id.toString());
        CustomApiResponse<Map<String, String> > successResponse = CustomApiResponse.success("상품 등록 완료", responseData);

        return ResponseEntity.status(HttpStatus.OK).body(successResponse);
    }

}
