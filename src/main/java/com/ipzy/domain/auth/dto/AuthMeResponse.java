package com.ipzy.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * /api/auth/me 응답 DTO - 현재 로그인한 사용자 정보
 */
@Schema(description = "로그인 사용자 정보")
public record AuthMeResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "이메일", example = "user@kakao.com")
        String email,

        @Schema(description = "사용자 이름", example = "홍길동")
        String name,

        @Schema(description = "프로필 이미지 URL", example = "https://k.kakaocdn.net/dn/example/profile.jpg")
        String profileImageUrl
) {

    public static AuthMeResponse from(CustomUserPrincipal principal) {
        return new AuthMeResponse(
                principal.getUserId(),
                principal.getEmail(),
                principal.getUserName(),
                principal.getProfileImageUrl()
        );
    }
}
