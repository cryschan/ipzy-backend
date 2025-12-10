package com.ipzy.domain.auth.controller;

import com.ipzy.domain.auth.dto.AuthMeResponse;
import com.ipzy.domain.auth.dto.CustomUserPrincipal;
import com.ipzy.domain.auth.exception.AuthException;
import com.ipzy._global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @Operation(
            summary = "카카오 로그인",
            description = """
                    카카오 OAuth2 로그인을 시작합니다.

                    **테스트 방법:**
                    - [카카오 로그인 바로가기](/api/auth/login/kakao)

                    브라우저에서 직접 접속하세요. Swagger에서 테스트 시 리다이렉트가 정상 동작하지 않을 수 있습니다.

                    **흐름:** `/api/auth/login/kakao` → 카카오 로그인 페이지 → 프론트엔드 리다이렉트

                    **성공 시:** 프론트엔드로 리다이렉트 (세션 쿠키 발급)

                    **실패 시:** 프론트엔드로 리다이렉트 + 쿼리 파라미터에 에러 코드
                    ```
                    {failure-redirect-uri}?code=AUTH_002
                    ```

                    **OAuth 콜백 에러 코드:**
                    | 코드 | HTTP | 설명 |
                    |------|------|------|
                    | AUTH_002 | 401 | OAuth 인증에 실패했습니다 |
                    | AUTH_006 | 401 | 사용자가 로그인을 취소했습니다 |
                    | AUTH_007 | 401 | OAuth 토큰이 유효하지 않습니다 |
                    | AUTH_008 | 401 | OAuth 응답을 처리할 수 없습니다 |
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "/oauth2/authorization/kakao로 리다이렉트"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "OAuth 콜백 실패 (AUTH_002, AUTH_006, AUTH_007, AUTH_008)")
    })
    @GetMapping("/login/kakao")
    public void kakaoLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/kakao");
    }

    @Operation(
            summary = "내 정보 조회",
            description = """
                    현재 로그인된 사용자 정보를 반환합니다.

                    **에러 코드:**
                    | 코드 | HTTP | 설명 |
                    |------|------|------|
                    | AUTH_001 | 401 | 인증이 필요합니다 |
                    | AUTH_005 | 401 | 세션이 만료되었습니다 |
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "userId": 1,
                                        "email": "user@example.com",
                                        "name": "홍길동",
                                        "profileImageUrl": "https://k.kakaocdn.net/..."
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않음 (AUTH_001, AUTH_005)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "AUTH_001",
                                        "message": "인증이 필요합니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/me")
    public ApiResponse<AuthMeResponse> me(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        if (principal == null) {
            throw AuthException.unauthorized();
        }

        return ApiResponse.success(AuthMeResponse.from(principal));
    }

    @Operation(
            summary = "로그아웃",
            description = """
                    세션을 무효화하고 카카오 계정 로그아웃 페이지로 리다이렉트합니다.

                    **테스트 방법:**
                    - [로그아웃 바로가기](/api/auth/logout)

                    브라우저에서 직접 접속하세요. Swagger에서 테스트 시 CORS 에러가 발생합니다.

                    **흐름:** `/api/auth/logout` → 카카오 로그아웃 → 프론트엔드 리다이렉트
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "카카오 로그아웃 페이지로 리다이렉트"
            )
    })
    @GetMapping("/logout")
    public void logout() {
        // Spring Security가 /api/auth/logout을 가로채서 처리함
        // 이 메서드는 Swagger 문서화 용도
    }
}
