package com.ipzy.domain.subscription.dto.Response;

import com.ipzy._global.common.enums.SubscriptionStatus;
import com.ipzy.domain.subscription.entity.Subscription;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 구독 상태 조회 응답 DTO
 */
@Schema(description = "사용자 구독 정보")
public record SubscriptionResponse(
        @Schema(description = "구독 ID", example = "1")
        Long id,

        @Schema(description = "플랜 정보")
        SubscriptionPlanResponse plan,

        @Schema(description = "구독 상태", example = "ACTIVE")
        SubscriptionStatus status,

        @Schema(description = "시작일", example = "2024-01-01T00:00:00")
        LocalDateTime startDate,

        @Schema(description = "종료일", example = "2024-02-01T00:00:00")
        LocalDateTime endDate,

        @Schema(description = "자동 갱신 여부", example = "true")
        Boolean autoRenew,

        @Schema(description = "활성화 여부", example = "true")
        boolean isActive,

        @Schema(description = "만료 여부", example = "false")
        boolean isExpired,

        @Schema(description = "취소일", example = "2024-01-15T00:00:00")
        LocalDateTime cancelledAt,

        @Schema(description = "취소 사유", example = "서비스 불만족")
        String cancelReason
) {

    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                SubscriptionPlanResponse.from(subscription.getPlan()),
                subscription.getStatus(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.getAutoRenew(),
                subscription.isActive(),
                subscription.isExpired(),
                subscription.getCancelledAt(),
                subscription.getCancelReason()
        );
    }
}
