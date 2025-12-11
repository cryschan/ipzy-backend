package com.ipzy.domain.subscription.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 구독 생성 요청 DTO
 */
@Schema(description = "구독 생성 요청")
public record CreateSubscriptionRequest(
        @Schema(description = "플랜 ID", example = "1", required = true)
        @NotNull(message = "플랜 ID는 필수입니다")
        Long planId
) {
}
