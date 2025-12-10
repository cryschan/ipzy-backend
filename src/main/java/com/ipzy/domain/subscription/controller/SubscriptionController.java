package com.ipzy.domain.subscription.controller;

import com.ipzy.domain.subscription.dto.Request.CreateSubscriptionRequest;
import com.ipzy.domain.subscription.dto.Response.SubscriptionPlanResponse;
import com.ipzy.domain.subscription.dto.Response.SubscriptionResponse;
import com.ipzy.domain.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
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
    @GetMapping("/subscription-plans")
    public ResponseEntity<List<SubscriptionPlanResponse>> getPlans() {
        List<SubscriptionPlanResponse> plans = subscriptionService.findAllPlans();
        return ResponseEntity.ok(plans);
    }

    /**
     * 내 구독 조회
     */
    @GetMapping("/{user_id}")
    public SubscriptionResponse getMySubscription() {
        return null;
    }

    /**
     * 구독 신청
     */
    @PostMapping("/subscriptions")
    public SubscriptionResponse createSubscription(@RequestBody CreateSubscriptionRequest request) {
        return null;
    }
}
