package com.ipzy.domain.subscription.controller;

import com.ipzy.domain.subscription.dto.Request.CreateSubscriptionRequest;
import com.ipzy.domain.subscription.dto.Response.SubscriptionPlanResponse;
import com.ipzy.domain.subscription.dto.Response.SubscriptionResponse;
import com.ipzy.domain.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
                    description = "플랜 목록 조회 성공"
            )
    })
    @GetMapping("/subscription-plans")
    public ResponseEntity<List<SubscriptionPlanResponse>> getPlans() {
        List<SubscriptionPlanResponse> plans = subscriptionService.findAllPlans();
        return ResponseEntity.ok(plans);
    }

    /**
     * 내 구독 조회
     */
    @Operation(summary = "내 구독 조회", description = "현재 로그인한 사용자의 구독 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "구독 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않음 (AUTH_001)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "활성 구독이 없음"
            )
    })
    @GetMapping("/me")
    public SubscriptionResponse getMySubscription() {
        return null;
    }

    /**
     * 구독 신청
     */
    @Operation(summary = "구독 신청", description = "선택한 플랜으로 구독을 신청합니다. Free 플랜은 즉시 활성화되며, 유료 플랜은 결제가 필요합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "구독하기 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이미 활성 구독이 존재함 또는 잘못된 요청"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않음 (AUTH_001)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "플랜을 찾을 수 없음"
            )
    })
    @PostMapping("/subscriptions")
    public SubscriptionResponse createSubscription(@RequestBody CreateSubscriptionRequest request) {
        return null;
    }
}
