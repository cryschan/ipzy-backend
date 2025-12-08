package com.ipzy.domain.auth.service;

import com.ipzy.domain.auth.dto.CustomUserPrincipal;
import com.ipzy.domain.auth.dto.oauth.KakaoOAuth2UserInfo;
import com.ipzy.domain.auth.dto.oauth.OAuth2UserInfo;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.repository.UserRepository;
import com.ipzy._global.common.enums.UserRole;
import com.ipzy._global.common.enums.UserStatus;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuth2UserInfoFactory oAuth2UserInfoFactory;

    private CustomOAuth2UserService customOAuth2UserService;

    @BeforeEach
    void setUp() {
        customOAuth2UserService = spy(new CustomOAuth2UserService(userRepository, oAuth2UserInfoFactory));
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
            OAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(kakaoOAuth2User.getAttributes());

            // fetchOAuth2User() mock - HTTP 호출 없이 mock 데이터 반환
            doReturn(kakaoOAuth2User).when(customOAuth2UserService).fetchOAuth2User(request);
            given(oAuth2UserInfoFactory.create(eq("kakao"), anyMap())).willReturn(userInfo);

            given(userRepository.findByProviderAndProviderId("KAKAO", "12345"))
                    .willReturn(Optional.empty());
            given(userRepository.findByEmail("test@kakao.com"))
                    .willReturn(Optional.empty());

            User savedUser = createNewUser(1L);  // 신규 사용자용
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            OAuth2User result = customOAuth2UserService.loadUser(request);

            // then
            verify(userRepository).save(any(User.class));
            assertThat(result).isInstanceOf(CustomUserPrincipal.class);
            CustomUserPrincipal principal = (CustomUserPrincipal) result;
            assertThat(principal.getUserId()).isEqualTo(1L);
            assertThat(principal.getEmail()).isEqualTo("test@kakao.com");
            assertThat(principal.getUserName()).isEqualTo("테스트유저");
        }

        @Test
        @DisplayName("기존 사용자 - 정보 갱신하고 OAuth2User 반환")
        void existingUser_updatesInfoAndReturnsOAuth2User() {
            // given
            OAuth2UserRequest request = createOAuth2UserRequest();
            OAuth2User kakaoOAuth2User = createKakaoOAuth2User();
            OAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(kakaoOAuth2User.getAttributes());

            doReturn(kakaoOAuth2User).when(customOAuth2UserService).fetchOAuth2User(request);
            given(oAuth2UserInfoFactory.create(eq("kakao"), anyMap())).willReturn(userInfo);

            User existingUser = createUser(1L);
            given(userRepository.findByProviderAndProviderId("KAKAO", "12345"))
                    .willReturn(Optional.of(existingUser));

            // when
            OAuth2User result = customOAuth2UserService.loadUser(request);

            // then
            verify(userRepository, never()).save(any(User.class));
            assertThat(existingUser.getName()).isEqualTo("테스트유저");
            assertThat(existingUser.getProfileImageUrl()).isEqualTo("https://example.com/image.png");
            assertThat(result).isInstanceOf(CustomUserPrincipal.class);
            CustomUserPrincipal principal = (CustomUserPrincipal) result;
            assertThat(principal.getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("로그인 시 lastLoginAt 갱신")
        void updatesLastLoginAt() {
            // given
            OAuth2UserRequest request = createOAuth2UserRequest();
            OAuth2User kakaoOAuth2User = createKakaoOAuth2User();
            OAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(kakaoOAuth2User.getAttributes());

            doReturn(kakaoOAuth2User).when(customOAuth2UserService).fetchOAuth2User(request);
            given(oAuth2UserInfoFactory.create(eq("kakao"), anyMap())).willReturn(userInfo);

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
            OAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(kakaoOAuth2User.getAttributes());

            doReturn(kakaoOAuth2User).when(customOAuth2UserService).fetchOAuth2User(request);
            given(oAuth2UserInfoFactory.create(eq("kakao"), anyMap())).willReturn(userInfo);

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
        @DisplayName("providerId가 없으면 OAuth2AuthenticationException 발생")
        void throwsException_whenProviderIdMissing() {
            // given
            OAuth2UserRequest request = createOAuth2UserRequest();
            OAuth2User invalidOAuth2User = createOAuth2UserWithoutKakaoAccount();

            doReturn(invalidOAuth2User).when(customOAuth2UserService).fetchOAuth2User(request);

            // providerId가 null인 UserInfo mock
            OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
            given(userInfo.getProviderId()).willReturn(null);
            given(oAuth2UserInfoFactory.create(eq("kakao"), anyMap())).willReturn(userInfo);

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

            // 이메일이 null인 UserInfo mock
            OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
            given(userInfo.getProviderId()).willReturn("12345");
            given(userInfo.getEmail()).willReturn(null);
            given(oAuth2UserInfoFactory.create(eq("kakao"), anyMap())).willReturn(userInfo);

            // when & then
            assertThatThrownBy(() -> customOAuth2UserService.loadUser(request))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .satisfies(ex -> {
                        OAuth2AuthenticationException oauthEx = (OAuth2AuthenticationException) ex;
                        assertThat(oauthEx.getError().getErrorCode()).isEqualTo("invalid_user_info");
                    });
        }

        @Test
        @DisplayName("같은 이메일의 다른 소셜로 로그인 시 계정 연동")
        void linksAccountWhenSameEmailDifferentProvider() {
            // given
            OAuth2UserRequest request = createOAuth2UserRequest();
            OAuth2User kakaoOAuth2User = createKakaoOAuth2User();
            OAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(kakaoOAuth2User.getAttributes());

            doReturn(kakaoOAuth2User).when(customOAuth2UserService).fetchOAuth2User(request);
            given(oAuth2UserInfoFactory.create(eq("kakao"), anyMap())).willReturn(userInfo);

            // provider+providerId로는 못 찾고
            given(userRepository.findByProviderAndProviderId("KAKAO", "12345"))
                    .willReturn(Optional.empty());

            // email로 찾으면 GOOGLE로 가입된 기존 사용자 발견
            User existingGoogleUser = createUserWithProvider(1L, "GOOGLE", "google123");
            given(userRepository.findByEmail("test@kakao.com"))
                    .willReturn(Optional.of(existingGoogleUser));

            // when
            OAuth2User result = customOAuth2UserService.loadUser(request);

            // then
            // 기존 계정에 KAKAO로 연동됨
            assertThat(existingGoogleUser.getProvider()).isEqualTo("KAKAO");
            assertThat(existingGoogleUser.getProviderId()).isEqualTo("12345");
            verify(userRepository, never()).save(any(User.class));
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
        return createUserWithInfo(id, "기존이름", "https://old-image.com/img.png");
    }

    private User createNewUser(Long id) {
        return createUserWithInfo(id, "테스트유저", "https://example.com/image.png");
    }

    private User createUserWithInfo(Long id, String name, String profileImageUrl) {
        User user = User.builder()
                .email("test@kakao.com")
                .name(name)
                .provider("KAKAO")
                .providerId("12345")
                .profileImageUrl(profileImageUrl)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private User createUserWithProvider(Long id, String provider, String providerId) {
        User user = User.builder()
                .email("test@kakao.com")
                .name("기존유저")
                .provider(provider)
                .providerId(providerId)
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
