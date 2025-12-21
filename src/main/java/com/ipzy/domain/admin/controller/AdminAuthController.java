package com.ipzy.domain.admin.controller;

import com.ipzy._global.common.ApiResponse;
import com.ipzy.domain.admin.dto.AdminLoginRequest;
import com.ipzy.domain.admin.dto.AdminLoginResponse;
import com.ipzy.domain.admin.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 인증 API
 */
@Tag(name = "Admin Auth", description = "관리자 인증 API")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "관리자 로그인", description = "이메일과 비밀번호로 관리자 로그인")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "로그인 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "id": 1,
                        "email": "admin@ipzy.com",
                        "name": "관리자",
                        "role": "ADMIN"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "유효성 검증 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "VALIDATION_ERROR",
                        "message": "이메일은 필수입니다"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패 (이메일/비밀번호 오류)",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_001",
                        "message": "이메일 또는 비밀번호가 올바르지 않습니다"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "접근 거부",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "관리자 아님",
                        value = """
                            {
                              "success": false,
                              "error": {
                                "code": "ADMIN_002",
                                "message": "관리자 권한이 없습니다"
                              }
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "정지된 계정",
                        value = """
                            {
                              "success": false,
                              "error": {
                                "code": "ADMIN_004",
                                "message": "정지된 계정입니다"
                              }
                            }
                            """
                    )
                }
            )
        )
    })
    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(
        @Valid @RequestBody AdminLoginRequest request,
        HttpSession session
    ) {
        AdminLoginResponse response = adminAuthService.login(request, session);
        return ApiResponse.success(response);
    }

    @Operation(summary = "관리자 로그아웃", description = "현재 세션 종료")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "로그아웃 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": null
                    }
                    """)
            )
        )
    })
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        adminAuthService.logout(session);
        return ApiResponse.success(null);
    }

    @Operation(summary = "현재 관리자 정보", description = "로그인된 관리자 정보 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "id": 1,
                        "email": "admin@ipzy.com",
                        "name": "관리자",
                        "role": "ADMIN"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "로그인 필요",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_005",
                        "message": "로그인이 필요합니다"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "관리자 권한 없음 (권한 박탈된 경우)",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_002",
                        "message": "관리자 권한이 없습니다"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "관리자를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_003",
                        "message": "관리자를 찾을 수 없습니다"
                      }
                    }
                    """)
            )
        )
    })
    @GetMapping("/me")
    public ApiResponse<AdminLoginResponse> getCurrentAdmin(HttpSession session) {
        AdminLoginResponse response = adminAuthService.getCurrentAdmin(session);
        return ApiResponse.success(response);
    }
}
