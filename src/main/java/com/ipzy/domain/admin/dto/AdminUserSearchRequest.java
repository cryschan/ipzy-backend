package com.ipzy.domain.admin.dto;

import com.ipzy._global.common.enums.UserRole;
import com.ipzy._global.common.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 관리자용 회원 검색 요청 DTO
 */
@Schema(description = "회원 검색 조건")
public record AdminUserSearchRequest(
        @Schema(description = "검색어 (이메일, 이름)", example = "")
        String keyword,

        @Schema(description = "회원 상태", example = "ACTIVE")
        UserStatus status,

        @Schema(description = "역할", example = "USER")
        UserRole role,

        @Schema(description = "가입일 시작", example = "2025-01-01")
        LocalDate createdFrom,

        @Schema(description = "가입일 종료", example = "2025-12-31")
        LocalDate createdTo
) {
}
