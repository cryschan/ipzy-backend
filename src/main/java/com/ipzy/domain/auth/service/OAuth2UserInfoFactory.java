package com.ipzy.domain.auth.service;

import com.ipzy.domain.auth.dto.oauth.KakaoOAuth2UserInfo;
import com.ipzy.domain.auth.dto.oauth.OAuth2UserInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * OAuth2 Provider별 UserInfo 객체 생성 팩토리.
 *
 * <p>registrationId에 따라 적절한 OAuth2UserInfo 구현체를 반환합니다.
 */
@Component
public class OAuth2UserInfoFactory {

    /**
     * Provider에 맞는 OAuth2UserInfo 구현체를 생성합니다.
     *
     * @param registrationId OAuth2 클라이언트 등록 ID (kakao)
     * @param attributes     OAuth2 Provider로부터 받은 사용자 속성
     * @return Provider별 OAuth2UserInfo 구현체
     * @throws IllegalArgumentException 지원하지 않는 Provider인 경우
     */
    public OAuth2UserInfo create(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> new KakaoOAuth2UserInfo(attributes);
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 OAuth2 Provider입니다: " + registrationId
            );
        };
    }
}
