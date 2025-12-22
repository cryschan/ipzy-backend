package com.ipzy.domain.admin.dto;

import com.ipzy.domain.subscription.entity.Subscription;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Schema(description = "관리자용 구독 목록 정보")
public record AdminSubscriptionResponse(
    @Schema(description = "구독 ID", example = "s1")
    String id,

    @Schema(description = "사용자 ID", example = "1")
    String userId,

    @Schema(description = "사용자 이름", example = "홍길동")
    String userName,

    @Schema(description = "사용자 이메일", example = "hong@email.com")
    String userEmail,

    @Schema(description = "플랜 (basic, pro)", example = "pro")
    String plan,

    @Schema(description = "구독 상태 (active, cancelled, expired)", example = "active")
    String status,

    @Schema(description = "시작일 (yyyy-MM-dd)", example = "2025-01-15")
    String startDate,

    @Schema(description = "종료일 (yyyy-MM-dd)", example = "2025-02-15")
    String endDate,

    @Schema(description = "결제 금액", example = "19900")
    Integer amount
) {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static AdminSubscriptionResponse from(Subscription subscription) {
        String statusValue = switch (subscription.getStatus()) {
            case ACTIVE -> "active";
            case CANCELLED -> "cancelled";
            case EXPIRED -> "expired";
            default -> subscription.getStatus().name().toLowerCase();
        };

        String planValue = subscription.getPlan().getName().toLowerCase();
        if (!planValue.equals("basic") && !planValue.equals("pro")) {
            planValue = "basic"; // fallback
        }

        return new AdminSubscriptionResponse(
            "s" + subscription.getId(),
            String.valueOf(subscription.getUser().getId()),
            subscription.getUser().getName(),
            subscription.getUser().getEmail(),
            planValue,
            statusValue,
            formatDate(subscription.getStartDate()),
            formatDate(subscription.getEndDate()),
            subscription.getPlan().getPrice()
        );
    }

    private static String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DATE_FORMAT);
    }
}
