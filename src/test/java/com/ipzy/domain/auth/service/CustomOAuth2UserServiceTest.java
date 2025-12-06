package com.ipzy.domain.auth.service;

import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.repository.UserRepository;
import com.ipzy.global.common.enums.UserRole;
import com.ipzy.global.common.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomOAuth2UserService customOAuth2UserService;

    @BeforeEach
    void setUp() {
        customOAuth2UserService = spy(new CustomOAuth2UserService(userRepository));
    }

    @Nested
    @DisplayName("loadUser")
    class LoadUser {

        @Test
        @DisplayName("신규 사용자 - 새로 저장하고 OAuth2User 반환")
        void newUser_savesAndReturnsOAuth2User() {
            // given
            OAuth2UserRequest request = createOAuth2UserRequest();
            OAuth2User kakaoOAuth2User = createKakaoOAuth2User();

            // fetchOAuth2User() mock - HTTP 호출 없이 mock 데이터 반환
            doReturn(kakaoOAuth2User).when(customOAuth2UserService).fetchOAuth2User(request);

            given(userRepository.findByProviderAndProviderId("KAKAO", "12345"))
                    .willReturn(Optional.empty());

            User savedUser = createUser(1L);
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            OAuth2User result = customOAuth2UserService.loadUser(request);

            // then
            verify(userRepository).save(any(User.class));
            assertThat(result.getAttributes().get("userId")).isEqualTo(1L);
            assertThat(result.getAttributes().get("email")).isEqualTo("test@kakao.com");
            assertThat(result.getAttributes().get("name")).isEqualTo("테스트유저");
        }

        @Test
        @DisplayName("기존 사용자 - 정보 갱신하고 OAuth2User 반환")
        void existingUser_updatesInfoAndReturnsOAuth2User() {
            // given
            OAuth2UserRequest request = createOAuth2UserRequest();
            OAuth2User kakaoOAuth2User = createKakaoOAuth2User();

            doReturn(kakaoOAuth2User).when(customOAuth2UserService).fetchOAuth2User(request);

            User existingUser = createUser(1L);
            given(userRepository.findByProviderAndProviderId("KAKAO", "12345"))
                    .willReturn(Optional.of(existingUser));

            // when
            OAuth2User result = customOAuth2UserService.loadUser(request);

            // then
            verify(userRepository, never()).save(any(User.class));
            assertThat(existingUser.getName()).isEqualTo("테스트유저");
            assertThat(existingUser.getProfileImageUrl()).isEqualTo("https://example.com/image.png");
            assertThat(result.getAttributes().get("userId")).isEqualTo(1L);
        }

        @Test
        @DisplayName("로그인 시 lastLoginAt 갱신")
        void updatesLastLoginAt() {
            // given
            OAuth2UserRequest request = createOAuth2UserRequest();
            OAuth2User kakaoOAuth2User = createKakaoOAuth2User();

            doReturn(kakaoOAuth2User).when(customOAuth2UserService).fetchOAuth2User(request);

            User existingUser = createUser(1L);
            assertThat(existingUser.getLastLoginAt()).isNull();

            given(userRepository.findByProviderAndProviderId("KAKAO", "12345"))
                    .willReturn(Optional.of(existingUser));

            // when
            customOAuth2UserService.loadUser(request);

            // then
            assertThat(existingUser.getLastLoginAt()).isNotNull();
        }

        @Test
        @DisplayName("OAuth2User에 올바른 권한 설정")
        void setsCorrectAuthority() {
            // given
            OAuth2UserRequest request = createOAuth2UserRequest();
            OAuth2User kakaoOAuth2User = createKakaoOAuth2User();

            doReturn(kakaoOAuth2User).when(customOAuth2UserService).fetchOAuth2User(request);

            User existingUser = createUser(1L);
            given(userRepository.findByProviderAndProviderId("KAKAO", "12345"))
                    .willReturn(Optional.of(existingUser));

            // when
            OAuth2User result = customOAuth2UserService.loadUser(request);

            // then
            assertThat(result.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("kakao_account가 없으면 OAuth2AuthenticationException 발생")
        void throwsException_whenKakaoAccountMissing() {
            // given
            OAuth2UserRequest request = createOAuth2UserRequest();
            OAuth2User invalidOAuth2User = createOAuth2UserWithoutKakaoAccount();

            doReturn(invalidOAuth2User).when(customOAuth2UserService).fetchOAuth2User(request);

            // when & then
            assertThatThrownBy(() -> customOAuth2UserService.loadUser(request))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .satisfies(ex -> {
                        OAuth2AuthenticationException oauthEx = (OAuth2AuthenticationException) ex;
                        assertThat(oauthEx.getError().getErrorCode()).isEqualTo("invalid_response");
                    });
        }

        @Test
        @DisplayName("profile이 없으면 OAuth2AuthenticationException 발생")
        void throwsException_whenProfileMissing() {
            // given
            OAuth2UserRequest request = createOAuth2UserRequest();
            OAuth2User invalidOAuth2User = createOAuth2UserWithoutProfile();

            doReturn(invalidOAuth2User).when(customOAuth2UserService).fetchOAuth2User(request);

            // when & then
            assertThatThrownBy(() -> customOAuth2UserService.loadUser(request))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .satisfies(ex -> {
                        OAuth2AuthenticationException oauthEx = (OAuth2AuthenticationException) ex;
                        assertThat(oauthEx.getError().getErrorCode()).isEqualTo("invalid_response");
                    });
        }

        @Test
        @DisplayName("이메일이 없으면 OAuth2AuthenticationException 발생")
        void throwsException_whenEmailMissing() {
            // given
            OAuth2UserRequest request = createOAuth2UserRequest();
            OAuth2User invalidOAuth2User = createOAuth2UserWithoutEmail();

            doReturn(invalidOAuth2User).when(customOAuth2UserService).fetchOAuth2User(request);

            // when & then
            assertThatThrownBy(() -> customOAuth2UserService.loadUser(request))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .satisfies(ex -> {
                        OAuth2AuthenticationException oauthEx = (OAuth2AuthenticationException) ex;
                        assertThat(oauthEx.getError().getErrorCode()).isEqualTo("invalid_user_info");
                    });
        }
    }

    private OAuth2UserRequest createOAuth2UserRequest() {
        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId("kakao")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientId("test-client-id")
                .redirectUri("http://localhost/callback")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        return new OAuth2UserRequest(clientRegistration, accessToken);
    }

    private OAuth2User createKakaoOAuth2User() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("nickname", "테스트유저");
        profile.put("profile_image_url", "https://example.com/image.png");

        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "test@kakao.com");
        kakaoAccount.put("profile", profile);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);
        attributes.put("kakao_account", kakaoAccount);

        return new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                "id"
        );
    }

    private User createUser(Long id) {
        User user = User.builder()
                .email("test@kakao.com")
                .name("기존이름")
                .provider("KAKAO")
                .providerId("12345")
                .profileImageUrl("https://old-image.com/img.png")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private OAuth2User createOAuth2UserWithoutKakaoAccount() {
        // kakao_account가 없는 응답
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);
        // kakao_account 없음

        return new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                "id"
        );
    }

    private OAuth2User createOAuth2UserWithoutProfile() {
        // profile이 없는 응답
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "test@kakao.com");
        // profile 없음

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);
        attributes.put("kakao_account", kakaoAccount);

        return new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                "id"
        );
    }

    private OAuth2User createOAuth2UserWithoutEmail() {
        // email이 없는 응답
        Map<String, Object> profile = new HashMap<>();
        profile.put("nickname", "테스트유저");
        profile.put("profile_image_url", "https://example.com/image.png");

        Map<String, Object> kakaoAccount = new HashMap<>();
        // email 없음
        kakaoAccount.put("profile", profile);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);
        attributes.put("kakao_account", kakaoAccount);

        return new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                "id"
        );
    }
}
