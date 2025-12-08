package com.ipzy.domain.auth.dto.oauth;

/**
 * OAuth2 Provider별 사용자 정보 추상화 인터페이스.
 *
 * <p>각 OAuth2 Provider(카카오, 네이버, 구글)의 응답 구조 차이를 캡슐화하여
 * 일관된 방식으로 사용자 정보에 접근할 수 있도록 합니다.
 */
public interface OAuth2UserInfo {

    /**
     * Provider 고유 사용자 ID를 반환합니다.
     *
     * @return Provider에서 발급한 사용자 식별자
     */
    String getProviderId();

    /**
     * 사용자 이메일을 반환합니다.
     *
     * @return 이메일 주소 (필수, null인 경우 인증 실패 처리)
     */
    String getEmail();

    /**
     * 사용자 이름(닉네임)을 반환합니다.
     *
     * @return 표시 이름 (nullable)
     */
    String getName();

    /**
     * 프로필 이미지 URL을 반환합니다.
     *
     * @return 프로필 이미지 URL (nullable)
     */
    String getProfileImageUrl();
}
