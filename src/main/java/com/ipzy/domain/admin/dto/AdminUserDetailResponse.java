package com.ipzy.domain.admin.dto;

import com.ipzy._global.common.enums.SubscriptionStatus;
import com.ipzy._global.common.enums.UserRole;
import com.ipzy._global.common.enums.UserStatus;
import com.ipzy.domain.subscription.entity.Subscription;
import com.ipzy.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 관리자용 회원 상세 응답 DTO
 */
@Schema(description = "관리자용 회원 상세 정보")
public record AdminUserDetailResponse(
        @Schema(description = "회원 ID", example = "1")
        Long id,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "회원 상태", example = "ACTIVE")
        UserStatus status,

        @Schema(description = "역할", example = "USER")
        UserRole role,

        @Schema(description = "OAuth 제공자", example = "KAKAO")
        String provider,

        @Schema(description = "OAuth 제공자 ID", example = "kakao_12345")
        String providerId,

        @Schema(description = "가입일")
        LocalDateTime createdAt,

        @Schema(description = "최근 로그인 일시")
        LocalDateTime lastLoginAt,

        @Schema(description = "구독 정보")
        SubscriptionSummary subscription
) {
    /**
     * 구독 요약 정보
     */
    @Schema(description = "구독 요약 정보")
    public record SubscriptionSummary(
            @Schema(description = "구독 ID", example = "1")
            Long id,

            @Schema(description = "플랜 이름", example = "PRO")
            String planName,

            @Schema(description = "플랜 표시명", example = "프로")
            String displayName,

            @Schema(description = "가격", example = "19900")
            Integer price,

            @Schema(description = "구독 상태", example = "ACTIVE")
            SubscriptionStatus status,

            @Schema(description = "시작일")
            LocalDateTime startDate,

            @Schema(description = "종료일")
            LocalDateTime endDate,

            @Schema(description = "자동 갱신 여부", example = "true")
            Boolean autoRenew
    ) {
        public static SubscriptionSummary from(Subscription subscription) {
            if (subscription == null) {
                return null;
            }
            return new SubscriptionSummary(
                    subscription.getId(),
                    subscription.getPlan().getName(),
                    subscription.getPlan().getDisplayName(),
                    subscription.getPlan().getPrice(),
                    subscription.getStatus(),
                    subscription.getStartDate(),
                    subscription.getEndDate(),
                    subscription.getAutoRenew()
            );
        }
    }

    public static AdminUserDetailResponse from(User user, Subscription subscription) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getStatus(),
                user.getRole(),
                user.getProvider(),
                user.getProviderId(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                SubscriptionSummary.from(subscription)
        );
    }
}
