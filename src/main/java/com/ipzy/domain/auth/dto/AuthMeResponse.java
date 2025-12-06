package com.ipzy.domain.auth.dto;

import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

/**
 * /api/auth/me 응답 DTO - 현재 로그인한 사용자 정보
 */
public record AuthMeResponse(
        Long userId,
        String email,
        String name
) {

    public static AuthMeResponse from(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        Long userId = attributes.get("userId") instanceof Number
                ? ((Number) attributes.get("userId")).longValue()
                : null;

        return new AuthMeResponse(
                userId,
                (String) attributes.get("email"),
                (String) attributes.get("name")
        );
    }
}
