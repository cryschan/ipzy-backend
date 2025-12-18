package com.ipzy.domain.admin.controller;

import com.ipzy._global.common.ApiResponse;
import com.ipzy.domain.admin.dto.AdminUserDetailResponse;
import com.ipzy.domain.admin.dto.AdminUserResponse;
import com.ipzy.domain.admin.dto.AdminUserSearchRequest;
import com.ipzy.domain.admin.dto.AdminUserStatusChangeRequest;
import com.ipzy.domain.admin.exception.AdminException;
import com.ipzy.domain.admin.service.AdminAuthService;
import com.ipzy.domain.admin.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자용 회원 관리 API
 */
@Tag(name = "Admin User", description = "관리자용 회원 관리 API")
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@RestController
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminAuthService adminAuthService;

    // TODO: Interceptor로 관리자 인증 공통 처리 필요

    @Operation(summary = "회원 목록 조회", description = "검색 조건에 따른 회원 목록 페이징 조회")
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
                        "content": [
                          {
                            "id": 1,
                            "email": "user@example.com",
                            "name": "홍길동",
                            "status": "ACTIVE",
                            "role": "USER",
                            "provider": "KAKAO",
                            "planName": "PRO",
                            "createdAt": "2024-01-01T00:00:00"
                          }
                        ],
                        "totalElements": 100,
                        "totalPages": 5,
                        "number": 0,
                        "size": 20
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
        )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(
            @ParameterObject @Valid AdminUserSearchRequest request,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpSession session
    ) {
        validateAdminSession(session);
        Page<AdminUserResponse> users = adminUserService.findUsers(request, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @Operation(summary = "회원 상세 조회", description = "특정 회원의 상세 정보 조회")
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
                        "email": "user@example.com",
                        "name": "홍길동",
                        "status": "ACTIVE",
                        "role": "USER",
                        "provider": "KAKAO",
                        "providerId": "kakao_12345",
                        "createdAt": "2024-01-01T00:00:00",
                        "lastLoginAt": "2024-12-01T10:30:00",
                        "subscription": {
                          "id": 1,
                          "planName": "PRO",
                          "displayName": "프로",
                          "price": 19900,
                          "status": "ACTIVE",
                          "startDate": "2024-01-01T00:00:00",
                          "endDate": "2025-01-01T00:00:00",
                          "autoRenew": true
                        }
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
            responseCode = "404",
            description = "회원을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_006",
                        "message": "회원을 찾을 수 없습니다"
                      }
                    }
                    """)
            )
        )
    })
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getUserDetail(
            @Parameter(description = "회원 ID", required = true, example = "1")
            @PathVariable Long userId,
            HttpSession session
    ) {
        validateAdminSession(session);
        AdminUserDetailResponse user = adminUserService.findUserDetail(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @Operation(summary = "회원 상태 변경", description = "회원 상태를 변경합니다 (정지, 활성화, 삭제 등)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "상태 변경 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "id": 1,
                        "email": "user@example.com",
                        "name": "홍길동",
                        "status": "SUSPENDED",
                        "role": "USER",
                        "provider": "KAKAO",
                        "providerId": "kakao_12345",
                        "createdAt": "2024-01-01T00:00:00",
                        "lastLoginAt": "2024-12-01T10:30:00",
                        "subscription": null
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
                        "message": "변경할 상태는 필수입니다"
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
            responseCode = "404",
            description = "회원을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_006",
                        "message": "회원을 찾을 수 없습니다"
                      }
                    }
                    """)
            )
        )
    })
    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> changeUserStatus(
            @Parameter(description = "회원 ID", required = true, example = "1")
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusChangeRequest request,
            HttpSession session
    ) {
        Long adminId = validateAdminSession(session);
        AdminUserDetailResponse user = adminUserService.changeUserStatus(userId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * 세션에서 관리자 ID 검증 및 반환
     */
    private Long validateAdminSession(HttpSession session) {
        Long adminId = adminAuthService.getAdminIdFromSession(session);
        if (adminId == null) {
            throw AdminException.sessionRequired();
        }
        return adminId;
    }
}
