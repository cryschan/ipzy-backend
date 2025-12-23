package com.ipzy.domain.admin.dto;

import com.ipzy._global.common.enums.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "구독 검색 조건")
public record AdminSubscriptionSearchRequest(
    @Schema(description = "검색어 (사용자 이메일, 이름)", example = "")
    String keyword,

    @Schema(description = "구독 상태", example = "ACTIVE")
    SubscriptionStatus status,

    @Schema(description = "플랜명", example = "PRO")
    String planName,

    @Schema(description = "구독일 시작", example = "2025-01-01")
    LocalDate createdFrom,

    @Schema(description = "구독일 종료", example = "2025-12-31")
    LocalDate createdTo
) {
}
