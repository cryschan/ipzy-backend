package com.ipzy.domain.auth.dto;

/**
 * /api/auth/me 응답 DTO - 현재 로그인한 사용자 정보
 */
public record AuthMeResponse(
        Long userId,
        String email,
        String name,
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
