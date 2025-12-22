package com.ipzy.domain.admin.dto;

import com.ipzy._global.common.enums.UserRole;
import com.ipzy._global.common.enums.UserStatus;
import com.ipzy.domain.subscription.entity.Subscription;
import com.ipzy.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 관리자용 회원 목록 응답 DTO
 */
@Schema(description = "관리자용 회원 목록 정보")
public record AdminUserResponse(
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

        @Schema(description = "구독 플랜명", example = "PRO")
        String planName,

        @Schema(description = "가입일")
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(User user, Subscription subscription) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getStatus(),
                user.getRole(),
                user.getProvider(),
                subscription != null ? subscription.getPlan().getName() : null,
                user.getCreatedAt()
        );
    }
}
