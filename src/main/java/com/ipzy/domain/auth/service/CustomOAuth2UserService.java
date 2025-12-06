package com.ipzy.domain.auth.service;

import com.ipzy.domain.auth.dto.CustomUserPrincipal;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.repository.UserRepository;
import com.ipzy.global.common.enums.UserRole;
import com.ipzy.global.common.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 카카오 OAuth2 로그인 시 사용자 정보를 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {

        OAuth2User oauth2User = fetchOAuth2User(request);

        String provider = request.getClientRegistration().getRegistrationId().toUpperCase();
        String providerId = String.valueOf(oauth2User.getAttributes().get("id"));

        // 카카오 응답에서 사용자 정보 추출
        Map<String, Object> kakaoAccount = (Map<String, Object>) oauth2User.getAttributes().get("kakao_account");

        if (kakaoAccount == null) {
            log.error("카카오 응답에 kakao_account가 없습니다. attributes: {}", oauth2User.getAttributes());
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_response", "카카오 계정 정보를 가져올 수 없습니다", null));
        }

        log.debug("kakaoAccount: {}", kakaoAccount);

        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        if (profile == null) {
            log.error("카카오 응답에 profile이 없습니다. kakaoAccount: {}", kakaoAccount);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_response", "카카오 프로필 정보를 가져올 수 없습니다", null));
        }

        log.debug("profile: {}", profile);

        String email = (String) kakaoAccount.get("email");

        if (email == null) {
            log.error("카카오 계정에 이메일 정보가 없습니다. providerId: {}", providerId);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info", "이메일 동의가 필수입니다", null));
        }

        String name = (String) profile.get("nickname");
        String profileImage = (String) profile.get("profile_image_url");

        // 사용자 조회 또는 생성 (기존 사용자는 OAuth 정보 갱신)
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .map(existingUser -> {
                    existingUser.updateOAuthInfo(name, profileImage);
                    return existingUser;
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .name(name)
                        .profileImageUrl(profileImage)
                        .provider(provider)
                        .providerId(providerId)
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
                        .build()));

        user.updateLastLoginAt();

        return CustomUserPrincipal.from(user, oauth2User.getAttributes());
    }

    /**
     * OAuth2 제공자로부터 사용자 정보를 가져옵니다.
     * 테스트에서 override 가능하도록 protected로 선언합니다.
     */
    protected OAuth2User fetchOAuth2User(OAuth2UserRequest request) {
        return super.loadUser(request);
    }
}
