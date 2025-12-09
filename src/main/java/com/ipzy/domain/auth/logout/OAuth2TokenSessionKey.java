package com.ipzy.domain.auth.logout;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Provider별 세션에 저장되는 OAuth2 access_token 키.
 */
@Getter
@RequiredArgsConstructor
public enum OAuth2TokenSessionKey {

    KAKAO("KAKAO", "OAUTH2_ACCESS_TOKEN_KAKAO"),
    NAVER("NAVER", "OAUTH2_ACCESS_TOKEN_NAVER"),
    GOOGLE("GOOGLE", "OAUTH2_ACCESS_TOKEN_GOOGLE");

    private final String provider;
    private final String sessionKey;

    /**
     * Provider 이름으로 세션 키를 반환합니다.
     *
     * @param provider Provider 이름 (KAKAO, NAVER, GOOGLE)
     * @return 해당 Provider의 세션 키
     */
    public static String getSessionKey(String provider) {
        for (OAuth2TokenSessionKey key : values()) {
            if (key.provider.equalsIgnoreCase(provider)) {
                return key.sessionKey;
            }
        }
        return "OAUTH2_ACCESS_TOKEN_" + provider.toUpperCase();
    }
}
