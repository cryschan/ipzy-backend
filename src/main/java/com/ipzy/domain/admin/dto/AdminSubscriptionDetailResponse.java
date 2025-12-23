package com.ipzy.domain.admin.dto;

import com.ipzy._global.common.enums.SubscriptionStatus;
import com.ipzy.domain.subscription.entity.Subscription;
import com.ipzy.domain.subscription.entity.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자용 구독 상세 정보")
public record AdminSubscriptionDetailResponse(
    @Schema(description = "구독 ID")
    Long id,

    @Schema(description = "사용자 정보")
    UserSummary user,

    @Schema(description = "플랜 정보")
    PlanSummary plan,

    @Schema(description = "구독 상태")
    SubscriptionStatus status,

    @Schema(description = "시작일")
    LocalDateTime startDate,

    @Schema(description = "종료일")
    LocalDateTime endDate,

    @Schema(description = "자동 갱신 여부")
    Boolean autoRenew,

    @Schema(description = "취소일")
    LocalDateTime cancelledAt,

    @Schema(description = "취소 사유")
    String cancelReason,

    @Schema(description = "생성일")
    LocalDateTime createdAt
) {
    @Schema(description = "사용자 요약 정보")
    public record UserSummary(
        Long id,
        String email,
        String name
    ) {
    }

    @Schema(description = "플랜 요약 정보")
    public record PlanSummary(
        Long id,
        String name,
        String displayName,
        Integer price,
        String billingPeriod
    ) {
        public static PlanSummary from(SubscriptionPlan plan) {
            return new PlanSummary(
                plan.getId(),
                plan.getName(),
                plan.getDisplayName(),
                plan.getPrice(),
                plan.getBillingPeriod() != null ? plan.getBillingPeriod().name() : null
            );
        }
    }

    public static AdminSubscriptionDetailResponse from(Subscription subscription) {
        return new AdminSubscriptionDetailResponse(
            subscription.getId(),
            new UserSummary(
                subscription.getUser().getId(),
                subscription.getUser().getEmail(),
                subscription.getUser().getName()
            ),
            PlanSummary.from(subscription.getPlan()),
            subscription.getStatus(),
            subscription.getStartDate(),
            subscription.getEndDate(),
            subscription.getAutoRenew(),
            subscription.getCancelledAt(),
            subscription.getCancelReason(),
            subscription.getCreatedAt()
        );
    }
}
