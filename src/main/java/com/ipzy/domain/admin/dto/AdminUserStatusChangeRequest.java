package com.ipzy.domain.admin.dto;

import com.ipzy._global.common.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 관리자용 회원 상태 변경 요청 DTO
 */
@Schema(description = "회원 상태 변경 요청")
public record AdminUserStatusChangeRequest(
        @Schema(description = "변경할 상태", example = "SUSPENDED", required = true)
        @NotNull(message = "변경할 상태는 필수입니다")
        UserStatus status,

        @Schema(description = "변경 사유", example = "서비스 이용 약관 위반")
        @Size(max = 500, message = "사유는 500자를 초과할 수 없습니다")
        String reason
) {
}
