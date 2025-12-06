package com.ipzy.domain.auth.controller;

import com.ipzy.domain.auth.dto.AuthMeResponse;
import com.ipzy.domain.auth.exception.AuthException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 인증 관련 API 컨트롤러 (로그인 시작, 현재 사용자 조회)
 */
@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Operation(summary = "카카오 로그인", description = "카카오 OAuth2 로그인 페이지로 리다이렉트합니다. 브라우저에서 직접 접속하세요.")
    @ApiResponse(responseCode = "302", description = "카카오 로그인 페이지로 리다이렉트")
    @GetMapping("/login/kakao")
    public void kakaoLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/kakao");
    }

    @Operation(summary = "내 정보 조회", description = "현재 로그인된 사용자 정보를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않음")
    })
    @GetMapping("/me")
    public com.ipzy.global.common.ApiResponse<AuthMeResponse> me(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            throw AuthException.unauthorized();
        }
        return com.ipzy.global.common.ApiResponse.success(AuthMeResponse.from(principal));
    }

    @Operation(summary = "로그아웃", description = "세션을 무효화하고 쿠키를 삭제합니다. 실제 처리는 Spring Security가 수행합니다.")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    @PostMapping("/logout")
    public void logout() {
        // Spring Security가 /api/auth/logout을 가로채서 처리함
        // 이 메서드는 Swagger 문서화 용도
    }
}
