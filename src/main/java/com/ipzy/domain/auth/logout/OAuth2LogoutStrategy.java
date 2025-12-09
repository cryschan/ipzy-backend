package com.ipzy.domain.auth.logout;

/**
 * OAuth2 Provider별 로그아웃(토큰 revoke) 전략 인터페이스.
 */
public interface OAuth2LogoutStrategy {

    /**
     * 지원하는 Provider 이름을 반환합니다.
     *
     * @return Provider 이름 (KAKAO, NAVER, GOOGLE)
     */
    String getProvider();

    /**
     * Provider의 토큰 revoke API를 호출합니다.
     *
     * <p>실패해도 예외를 던지지 않고 로깅만 수행합니다.
     * 우리 서버의 로그아웃(세션 무효화)은 계속 진행됩니다.
     *
     * @param accessToken OAuth2 access token
     */
    void revokeToken(String accessToken);
}
