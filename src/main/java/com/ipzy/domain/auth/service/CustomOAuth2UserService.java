package com.ipzy.domain.auth.service;

import com.ipzy.domain.auth.dto.CustomUserPrincipal;
import com.ipzy.domain.auth.dto.oauth.OAuth2UserInfo;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.repository.UserRepository;
import com.ipzy._global.common.enums.UserRole;
import com.ipzy._global.common.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth2 소셜 로그인 사용자 정보 처리 서비스.
 *
 * <p>카카오, 네이버, 구글 등 멀티 Provider를 지원합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final OAuth2UserInfoFactory oAuth2UserInfoFactory;

    // TODO: tato126 다양한 소셜 로그인 연동을 위해서는 테이블을 따로 분리해야 한다.
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = fetchOAuth2User(request);

        String registrationId = request.getClientRegistration().getRegistrationId();
        String provider = registrationId.toUpperCase();

        // Factory를 통해 Provider별 UserInfo 추출
        OAuth2UserInfo userInfo = oAuth2UserInfoFactory.create(
                registrationId,
                oauth2User.getAttributes()
        );

        // 필수 정보 검증
        validateUserInfo(userInfo, provider);

        String providerId = userInfo.getProviderId();
        String email = userInfo.getEmail();
        String name = userInfo.getName();
        String profileImage = userInfo.getProfileImageUrl();

        // 이름이 없으면 이메일 앞부분으로 대체
        if (name == null || name.isBlank()) {
            name = email.split("@")[0];
            log.info("{} 사용자 이름이 없어 이메일로 대체: {}", provider, name);
        }

        // TODO: tato126 사용자 조회 , 생성 로직을 분리해여 관리를 편하게 하도록 한다.
        // 사용자 조회 또는 생성 (provider+providerId 또는 email로 조회)
        final String finalName = name;
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .or(() -> userRepository.findByEmail(email))
                .map(existingUser -> {

                    // 같은 이메일이지만 다른 Provider → 소셜 연동
                    if (!existingUser.getProvider().equals(provider)) {
                        existingUser.linkSocialAccount(provider, providerId);
                        log.info("기존 계정에 {} 소셜 연동: userId={}", provider, existingUser.getId());
                    }

                    existingUser.updateOAuthInfo(finalName, profileImage);
                    return existingUser;
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .name(finalName)
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
     * 필수 사용자 정보를 검증합니다.
     */
    private void validateUserInfo(OAuth2UserInfo userInfo, String provider) {

        if (userInfo.getProviderId() == null) {

            log.error("{} 응답에 사용자 ID가 없습니다", provider);

            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_response",
                            provider + " 사용자 ID를 가져올 수 없습니다", null)
            );
        }

        if (userInfo.getEmail() == null) {

            log.error("{} 응답에 이메일이 없습니다. providerId: {}",
                    provider, userInfo.getProviderId());

            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info", "이메일 동의가 필수입니다", null)
            );
        }
    }

    /**
     * OAuth2 제공자로부터 사용자 정보를 가져옵니다.
     * 테스트에서 override 가능하도록 protected로 선언합니다.
     */
    protected OAuth2User fetchOAuth2User(OAuth2UserRequest request) {
        return super.loadUser(request);
    }
}
