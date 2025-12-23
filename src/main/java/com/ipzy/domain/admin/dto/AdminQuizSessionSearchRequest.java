package com.ipzy.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "퀴즈 세션 검색 조건")
public record AdminQuizSessionSearchRequest(
    @Schema(description = "검색어 (사용자 이메일, 이름)", example = "")
    String keyword,

    @Schema(description = "퀴즈 ID", example = "1")
    Long quizId,

    @Schema(description = "완료 여부")
    Boolean completed,

    @Schema(description = "시작일", example = "2025-01-01")
    LocalDate createdFrom,

    @Schema(description = "종료일", example = "2025-12-31")
    LocalDate createdTo
) {
}
