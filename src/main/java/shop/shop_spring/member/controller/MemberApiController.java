package shop.shop_spring.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import shop.shop_spring.common.response.CustomApiResponse;
import shop.shop_spring.member.dto.*;
import shop.shop_spring.member.MemberForm;
import shop.shop_spring.member.domain.Member;
import shop.shop_spring.member.service.MemberServiceImpl;
import shop.shop_spring.product.Dto.ProductUpdateRequest;
import shop.shop_spring.product.service.ProductService;
import shop.shop_spring.security.auth.AuthService;
import shop.shop_spring.security.model.MyUser;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "Member API", description = "회원 관련 API(회원 가입, 로그인, 회원 정보 수정 등)")
public class MemberApiController {
    private final MemberServiceImpl memberService;
    private final ProductService productService;
    private final AuthService authService;

    @Operation(summary = "로그인 기능", description = "사용자 인증 후 jwt 반환")
    @PostMapping("/login")
    @ResponseBody
    public String doLogin(@RequestBody LoginRequest request,
                          HttpServletResponse httpServletResponse) {
        String jwt = authService.login(request.getUsername(), request.getPassword());

        addJwtCookieToResponse(httpServletResponse, jwt);

        return jwt;
    }

    private void addJwtCookieToResponse(HttpServletResponse httpServletResponse, String jwt){
        // 쿠키에 jwt 저장
        var cookie = new Cookie("jwt", jwt);
        cookie.setMaxAge(60 * 60);
        cookie.setHttpOnly(true); // js 등에서 접근 x
        cookie.setPath("/");
        httpServletResponse.addCookie(cookie);
    }

    @Operation(summary = "등록 상품 정보 수정", description = "회원이 등록한 상품의 정보를 수정 후 저장")
    @PutMapping("/my-page/products/{id}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상품 정보 수정 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(유효하지 않은 값)"),
    })
    ResponseEntity editDetail(@PathVariable Long id, @RequestBody ProductUpdateRequest updateRequest, Authentication auth){
        MyUser user = (MyUser) auth.getPrincipal();
;
        productService.updateProduct(user.getUsername(), id, updateRequest);

        CustomApiResponse<Void> response = CustomApiResponse.successNoData("상품 정보 수정 완료");

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "등록 상품 삭제", description = "사용자가 등록한 상품을 삭제")
    @DeleteMapping("/my-page/products/{id}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상품 삭제 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(존재하지 않는 상품)"),
    })
    ResponseEntity deleteProduct(@PathVariable Long id, Authentication auth){
        MyUser user = (MyUser) auth.getPrincipal();

        productService.deleteProduct(user.getUsername(), id);

        CustomApiResponse<Void> response = CustomApiResponse.successNoData("상품 삭제 완료");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "회원 가입", description = "폼에 기입된 내용을 가진 회원 생성")
    @PostMapping
    public String signup(@ModelAttribute MemberForm form){
        MemberCreationRequest memberCreationRequest = formToMemberCreationRequest(form);
        memberService.createMember(memberCreationRequest);
        return "redirect:/members/login";
    }

    @Operation(summary = "회원 정보 수정", description = "회원의 개인 정보를 수정 후 저장")
    @PutMapping("/my-page/profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 정보 수정 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않은 수정 값)"),
    })
    public ResponseEntity<CustomApiResponse<Void>> modifyProfile(@RequestBody MemberUpdateRequest request){
        // validateMember(updateMemberDtoToMember(dto)) 검증 한번 하는게 좋을 듯
        memberService.updateMember(updateMemberDtoToMember(request));
        
        CustomApiResponse<Void> successResponse = CustomApiResponse.successNoData("회원 정보 수정 완료");
        
        return ResponseEntity.status(HttpStatus.OK).body(successResponse);
    }


    private Member updateMemberDtoToMember(MemberUpdateRequest request){
        Member member = new Member();
        member.setUsername(request.getUsername());
        member.setPassword(request.getPassword());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate localDate = null;
        try {
            localDate = LocalDate.parse(request.getBirthDate(), formatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("잘못된 날짜 형식");
        } catch (NullPointerException e){
            localDate = null;
        }
        member.setBirthDate(localDate);
        member.setAddress(request.getAddress());
        member.setAddressDetail(request.getAddressDetail());
        member.setNickname(request.getNickName());
        return member;
    }

    @Operation(summary = "비밀번호 초기화를 위한 본인 인증", description = "비밀번호를 초기화하기 위한 사용자 확인")
    @PostMapping("/password-reset")
    @ResponseBody
    public ResponseEntity resetPassword(@RequestBody PasswordResetRequest request){
        String username = request.getUsername();
        String name = request.getName();
        String brithDate = request.getBirthDate();

        memberService.validateUserInformation(username, name, brithDate);

        Map<String, String> responseData = CustomApiResponse.createResponseData("username", username);
        CustomApiResponse<Map<String, String>> successResponse =
                CustomApiResponse.success("본인 인증 완료", responseData);
        return ResponseEntity.status(HttpStatus.OK).body(successResponse);
    }

    @Operation(summary = "비밀번호 변경", description = "회원의 비밀번호를 변경")
    @PostMapping("/change-password")
    public ResponseEntity updatePassword(@RequestBody PasswordChangeRequest request){

        memberService.updatePassword(request.getUsername(),request.getNewPassword());

        CustomApiResponse<Void> response = CustomApiResponse.successNoData("비밀번호 변경 완료");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "이메일 검증을 위한 랜덤 숫자 전송", description = "이메일 유효성을 확인하기 위한 랜덤 숫자 이메일 전송")
    @PostMapping("/verify-email")
    public ResponseEntity<CustomApiResponse<Map<String, String>>> sendEmail(@RequestBody EmailVerificationRequest request) throws MessagingException, UnsupportedEncodingException {
        // 인증 번호 생성 및 메일 전송
        memberService.sendAuthenticationCode(request.getEmail());

        // 성공 시 응답 데이터 준비
        Map<String, String> responseData = CustomApiResponse.createResponseData("email", request.getEmail());
        CustomApiResponse<Map<String, String>> successResponse =
                CustomApiResponse.success("인증 번호 발송 성공", responseData);

        return ResponseEntity.status(HttpStatus.OK).body(successResponse);
    }

    @Operation(summary = "이메일 인증", description = "랜덤 숫자 비교를 통한 이메일 인증")
    @PostMapping("/validate-email")
    public ResponseEntity<CustomApiResponse<Map<String, String>>> validateEmail(@RequestBody EmailValidateRequest request) {
        memberService.validateAuthenticationCode(request.getEmail(), request.getCode());

        Map<String, String> responseData = CustomApiResponse.createResponseData("email", request.getEmail());
        CustomApiResponse<Map<String, String>> successResponse =
                CustomApiResponse.success("이메일 인증 성공", responseData);

        return ResponseEntity.status(200).body(successResponse);
    }

    private MemberCreationRequest formToMemberCreationRequest (MemberForm form) {
        MemberCreationRequest memberCreationRequest = new MemberCreationRequest();
        memberCreationRequest.setUsername(form.getEmail());
        memberCreationRequest.setPassword(form.getPassword());
        memberCreationRequest.setName(form.getName());
        memberCreationRequest.setAddress(form.getAddress());
        memberCreationRequest.setAddressDetail(form.getAddressDetail());
        memberCreationRequest.setBirthDate(form.getBirthDate());
        memberCreationRequest.setNickname(form.getNickname());
        return memberCreationRequest;
    }


}
