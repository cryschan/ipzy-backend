package com.ipzy.domain.subscription.dto.Response;

import com.ipzy._global.common.enums.BillingPeriod;
import com.ipzy.domain.subscription.entity.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * 구독 플랜 조회 응답 DTO
 */
@Schema(description = "구독 플랜 정보")
public record SubscriptionPlanResponse(
        @Schema(description = "플랜 ID", example = "1")
        Long id,

        @Schema(description = "플랜 이름", example = "BASIC")
        String name,

        @Schema(description = "표시 이름", example = "Basic")
        String displayName,

        @Schema(description = "가격", example = "9900")
        Integer price,

        @Schema(description = "통화", example = "KRW")
        String currency,

        @Schema(description = "결제 주기", example = "MONTHLY")
        BillingPeriod billingPeriod,

        @Schema(description = "플랜 기능", example = "{\"proxy_limit\": 100}")
        Map<String, Object> features,

        @Schema(description = "설명", example = "개인 사용자를 위한 베이직 플랜")
        String description,

        @Schema(description = "뱃지", example = "인기")
        String badge,

        @Schema(description = "결제 필요 여부", example = "true")
        boolean requiresPayment
) {

    public static SubscriptionPlanResponse from(SubscriptionPlan plan) {
        return new SubscriptionPlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getDisplayName(),
                plan.getPrice(),
                plan.getCurrency(),
                plan.getBillingPeriod(),
                plan.getFeatures(),
                plan.getDescription(),
                plan.getBadge(),
                plan.getPrice() > 0  // 가격이 0보다 크면 결제 필요
        );
    }
}
