package com.ipzy.domain.admin.controller;

import com.ipzy._global.common.ApiResponse;
import com.ipzy.domain.admin.dto.AdminSubscriptionDetailResponse;
import com.ipzy.domain.admin.dto.AdminSubscriptionResponse;
import com.ipzy.domain.admin.dto.AdminSubscriptionSearchRequest;
import com.ipzy.domain.admin.exception.AdminException;
import com.ipzy.domain.admin.service.AdminAuthService;
import com.ipzy.domain.admin.service.AdminSubscriptionService;
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

@Tag(name = "Admin Subscription", description = "관리자용 구독 관리 API")
@RestController
@RequestMapping("/api/admin/subscriptions")
@RequiredArgsConstructor
public class AdminSubscriptionController {

    private final AdminSubscriptionService adminSubscriptionService;
    private final AdminAuthService adminAuthService;

    @Operation(summary = "구독 목록 조회", description = "검색 조건에 따른 구독 목록 페이징 조회")
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
                            "userId": 1,
                            "userEmail": "user@example.com",
                            "userName": "홍길동",
                            "planName": "PRO",
                            "planDisplayName": "프로",
                            "status": "ACTIVE",
                            "startDate": "2024-01-01T00:00:00",
                            "endDate": "2025-01-01T00:00:00",
                            "autoRenew": true,
                            "createdAt": "2024-01-01T00:00:00"
                          }
                        ],
                        "totalElements": 100,
                        "totalPages": 5
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
    public ResponseEntity<ApiResponse<Page<AdminSubscriptionResponse>>> getSubscriptions(
            @ParameterObject @Valid AdminSubscriptionSearchRequest request,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpSession session
    ) {
        validateAdminSession(session);
        Page<AdminSubscriptionResponse> subscriptions = adminSubscriptionService.findSubscriptions(request, pageable);
        return ResponseEntity.ok(ApiResponse.success(subscriptions));
    }

    @Operation(summary = "구독 상세 조회", description = "특정 구독의 상세 정보 조회")
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
                        "user": {
                          "id": 1,
                          "email": "user@example.com",
                          "name": "홍길동"
                        },
                        "plan": {
                          "id": 2,
                          "name": "PRO",
                          "displayName": "프로",
                          "price": 19900,
                          "durationDays": 30
                        },
                        "status": "ACTIVE",
                        "startDate": "2024-01-01T00:00:00",
                        "endDate": "2025-01-01T00:00:00",
                        "autoRenew": true,
                        "createdAt": "2024-01-01T00:00:00"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "구독을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_010",
                        "message": "구독을 찾을 수 없습니다"
                      }
                    }
                    """)
            )
        )
    })
    @GetMapping("/{subscriptionId}")
    public ResponseEntity<ApiResponse<AdminSubscriptionDetailResponse>> getSubscriptionDetail(
            @Parameter(description = "구독 ID", required = true, example = "1")
            @PathVariable Long subscriptionId,
            HttpSession session
    ) {
        validateAdminSession(session);
        AdminSubscriptionDetailResponse subscription = adminSubscriptionService.findSubscriptionDetail(subscriptionId);
        return ResponseEntity.ok(ApiResponse.success(subscription));
    }

    @Operation(summary = "구독 취소", description = "활성화된 구독을 취소합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "취소 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "id": 1,
                        "status": "CANCELLED",
                        "cancelledAt": "2024-12-21T10:30:00",
                        "cancelReason": "관리자에 의한 취소"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "취소 불가능한 상태",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_011",
                        "message": "활성화된 구독만 취소할 수 있습니다"
                      }
                    }
                    """)
            )
        )
    })
    @PatchMapping("/{subscriptionId}/cancel")
    public ResponseEntity<ApiResponse<AdminSubscriptionDetailResponse>> cancelSubscription(
            @Parameter(description = "구독 ID", required = true, example = "1")
            @PathVariable Long subscriptionId,
            @Parameter(description = "취소 사유")
            @RequestParam(required = false) String reason,
            HttpSession session
    ) {
        Long adminId = validateAdminSession(session);
        AdminSubscriptionDetailResponse subscription = adminSubscriptionService.cancelSubscription(subscriptionId, reason, adminId);
        return ResponseEntity.ok(ApiResponse.success(subscription));
    }

    private Long validateAdminSession(HttpSession session) {
        Long adminId = adminAuthService.getAdminIdFromSession(session);
        if (adminId == null) {
            throw AdminException.sessionRequired();
        }
        return adminId;
    }
}
