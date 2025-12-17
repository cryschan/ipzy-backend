package com.ipzy.domain.admin.dto;

import com.ipzy._global.common.enums.UserRole;
import com.ipzy.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 로그인 응답")
public record AdminLoginResponse(
    @Schema(description = "관리자 ID")
    Long id,

    @Schema(description = "이메일")
    String email,

    @Schema(description = "이름")
    String name,

    @Schema(description = "역할", example = "ADMIN")
    UserRole role
) {
    public static AdminLoginResponse from(User user) {
        return new AdminLoginResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole()
        );
    }
}
