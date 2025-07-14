package shop.shop_spring.cart.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import shop.shop_spring.cart.dto.CartAddRequest;
import shop.shop_spring.cart.dto.CartItemUpdateRequest;
import shop.shop_spring.cart.service.CartServiceImpl;
import shop.shop_spring.common.response.CustomApiResponse;
import shop.shop_spring.security.model.MyUser;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart API", description = "장바구니 관련 API(추가, 삭제, 비우기 등)")
public class CartApiController {
    private final CartServiceImpl cartService;

    @Operation(summary = "장바구니 상품 추가", description = "사용자의 장바구니에 상품을 추가")
    @PostMapping("/items")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "추가 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청(없는 상품, 없는 회원 정보)"),
        @ApiResponse(responseCode = "403", description = "로그인 정보 불일치 또는 세션 종료")
    })
    public ResponseEntity addProductToCart(@RequestBody CartAddRequest addRequest, Authentication auth){
        MyUser member = (MyUser) auth.getPrincipal();

        cartService.addItemToCart(member.getId(), addRequest.getProductId(),addRequest.getQuantity());

        CustomApiResponse<Void> response = CustomApiResponse.successNoData("추가 성공");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "장바구니 내 특정 상품 삭제", description = "사용자의 장바구니에서 선택된 특정 상품을 제거")
    @DeleteMapping("/items/{id}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상품 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(없는 상품, 없는 회원 정보)"),
            @ApiResponse(responseCode = "403", description = "로그인 정보 불일치 또는 세션 종료")
    })
    public ResponseEntity removeProductFromCart(@PathVariable Long id, Authentication auth){
        MyUser member = (MyUser) auth.getPrincipal();
        cartService.removeItemFromCart(member.getId(), id);

        CustomApiResponse<Void> response = CustomApiResponse.successNoData("상품 삭제 성공");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "장바구니 비우기", description = "사용자의 장바구니 내에 있는 모든 상품 제거")
    @DeleteMapping
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "장바구니 비우기 성공 or 이미 비어있는 장바구니"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(없는 회원 정보)"),
            @ApiResponse(responseCode = "403", description = "로그인 정보 불일치 또는 세션 종료")
    })
    public ResponseEntity clearCart(Authentication auth){
        MyUser member = (MyUser) auth.getPrincipal();

        boolean cleared = cartService.clearCart(member.getId());

        String message = cleared ? "장바구니 비우기 성공" : "이미 비어있는 장바구니";

        CustomApiResponse<Void> response  = CustomApiResponse.successNoData(message);;

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "장바구니 내 상품 수량 업데이트", description = "장바구니 내에 있는 상품의 수량을 변경")
    @PutMapping("/items/{id}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상품 개수 업데이트"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(없는 상품, 잘못된 상품 수량)"),
            @ApiResponse(responseCode = "403", description = "로그인 정보 불일치 또는 세션 종료")
    })
    public ResponseEntity updateQuantity(@PathVariable Long id, @RequestBody CartItemUpdateRequest updateRequest, Authentication auth){
        MyUser member = (MyUser) auth.getPrincipal();
        cartService.updateItemQuantity(member.getId(), id ,updateRequest.getQuantity());

        CustomApiResponse<Void> response = CustomApiResponse.successNoData("상품 개수 업데이트");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
