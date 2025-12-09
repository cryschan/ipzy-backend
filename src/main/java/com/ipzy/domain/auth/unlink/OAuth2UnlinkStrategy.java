package com.ipzy.domain.auth.unlink;

/**
 * OAuth2 Provider별 연결 끊기(unlink) 전략 인터페이스.
 *
 * <p>계정 탈퇴 시 Provider와의 연결을 완전히 해제합니다.
 * 로그아웃(토큰 만료)과 달리, unlink 후에는 재로그인 시 동의 화면이 다시 표시됩니다.
 */
public interface OAuth2UnlinkStrategy {

    /**
     * 지원하는 Provider 이름을 반환합니다.
     *
     * @return Provider 이름 (KAKAO, NAVER, GOOGLE)
     */
    String getProvider();

    /**
     * Provider의 연결 끊기 API를 호출합니다.
     *
     * <p>실패해도 예외를 던지지 않고 로깅만 수행합니다.
     * 탈퇴 프로세스는 계속 진행됩니다.
     *
     * @param accessToken OAuth2 access token
     * @return 연결 끊기 성공 여부
     */
    boolean unlink(String accessToken);
}