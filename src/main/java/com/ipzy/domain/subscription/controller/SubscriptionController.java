package com.ipzy.domain.subscription.controller;

import com.ipzy._global.common.ApiResponse;
import com.ipzy.domain.subscription.dto.Request.CreateSubscriptionRequest;
import com.ipzy.domain.subscription.dto.Response.SubscriptionPlanResponse;
import com.ipzy.domain.subscription.dto.Response.SubscriptionResponse;
import com.ipzy.domain.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Subscription", description = "구독 관리 API")
@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * 플랜 목록 조회 - 사용자가 선택할 수 있는 플랜 보여주기
     */
    @Operation(summary = "구독 플랜 목록 조회", description = "사용자가 선택할 수 있는 모든 구독 플랜을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "플랜 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success",
                                    summary = "구독 플랜 목록 예시",
                                    value = """
                                            {
                                              "success": true,
                                              "data": [
                                                {
                                                  "id": 1,
                                                  "name": "FREE",
                                                  "displayName": "Free",
                                                  "price": 0,
                                                  "currency": "KRW",
                                                  "billingPeriod": "MONTHLY",
                                                  "features": {
                                                    "proxy_limit": 10,
                                                    "ai_tokens": 1000
                                                  },
                                                  "description": "개인 사용자를 위한 무료 플랜",
                                                  "badge": "무료",
                                                  "requiresPayment": false
                                                },
                                                {
                                                  "id": 2,
                                                  "name": "PRO",
                                                  "displayName": "Pro",
                                                  "price": 19900,
                                                  "currency": "KRW",
                                                  "billingPeriod": "MONTHLY",
                                                  "features": {
                                                    "proxy_limit": 100,
                                                    "ai_tokens": 10000
                                                  },
                                                  "description": "팀 사용자를 위한 프로 플랜",
                                                  "badge": "인기",
                                                  "requiresPayment": true
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "등록된 구독 플랜 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "PlanNotFound",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "SUBSCRIPTION_PLAN_NOT_FOUND",
                                                "message": "등록된 구독 플랜이 없습니다"
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/subscription-plans")
    public ApiResponse<List<SubscriptionPlanResponse>> getPlans() {
        List<SubscriptionPlanResponse> plans = subscriptionService.findAllPlans();
        return ApiResponse.success(plans);
    }

    /**
     * 내 구독 조회
     */
    @Operation(summary = "내 구독 조회", description = "현재 로그인한 사용자의 구독 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "구독 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success",
                                    summary = "내 구독 정보",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "id": 42,
                                                "plan": {
                                                  "id": 2,
                                                  "name": "PRO",
                                                  "displayName": "Pro",
                                                  "price": 19900,
                                                  "currency": "KRW",
                                                  "billingPeriod": "MONTHLY",
                                                  "features": {
                                                    "proxy_limit": 100,
                                                    "ai_tokens": 10000
                                                  },
                                                  "description": "팀 사용자를 위한 프로 플랜",
                                                  "badge": "인기",
                                                  "requiresPayment": true
                                                },
                                                "status": "ACTIVE",
                                                "startDate": "2024-01-01T00:00:00",
                                                "endDate": "2024-02-01T00:00:00",
                                                "autoRenew": true,
                                                "isActive": true,
                                                "isExpired": false,
                                                "cancelledAt": null,
                                                "cancelReason": null
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않음 (AUTH_001)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "AUTH_001",
                                                "message": "인증이 필요합니다"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "활성 구독이 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "NotFound",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "SUBSCRIPTION_NOT_FOUND",
                                                "message": "활성 구독이 없습니다"
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/me")
    public ApiResponse<SubscriptionResponse> getMySubscription() {
        return ApiResponse.success(null);
    }

    /**
     * 구독 신청
     */
    @Operation(summary = "구독 신청", description = "선택한 플랜으로 구독을 신청합니다. Free 플랜은 즉시 활성화되며, 유료 플랜은 결제가 필요합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "구독하기 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success",
                                    summary = "구독 생성 결과",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "id": 55,
                                                "plan": {
                                                  "id": 2,
                                                  "name": "PRO",
                                                  "displayName": "Pro",
                                                  "price": 19900,
                                                  "currency": "KRW",
                                                  "billingPeriod": "MONTHLY",
                                                  "features": {
                                                    "proxy_limit": 100,
                                                    "ai_tokens": 10000
                                                  },
                                                  "description": "팀 사용자를 위한 프로 플랜",
                                                  "badge": "인기",
                                                  "requiresPayment": true
                                                },
                                                "status": "PENDING",
                                                "startDate": "2024-01-01T00:00:00",
                                                "endDate": "2024-02-01T00:00:00",
                                                "autoRenew": true,
                                                "isActive": true,
                                                "isExpired": false,
                                                "cancelledAt": null,
                                                "cancelReason": null
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이미 활성 구독이 존재함 또는 잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "ActiveSubscription",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "SUBSCRIPTION_ACTIVE",
                                                "message": "이미 활성 구독이 존재합니다"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않음 (AUTH_001)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "AUTH_001",
                                                "message": "인증이 필요합니다"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "플랜을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "PlanNotFound",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "SUBSCRIPTION_PLAN_NOT_FOUND",
                                                "message": "선택한 플랜을 찾을 수 없습니다"
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/subscriptions")
    public ApiResponse<SubscriptionResponse> createSubscription(@RequestBody CreateSubscriptionRequest request) {
        return ApiResponse.success(null);
    }
}
