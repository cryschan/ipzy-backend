package com.ipzy.domain.auth.service;

import com.ipzy.domain.auth.dto.CustomUserPrincipal;
import com.ipzy.domain.auth.dto.oauth.KakaoOAuth2UserInfo;
import com.ipzy.domain.auth.dto.oauth.OAuth2UserInfo;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.service.UserService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private OAuth2UserInfoFactory oAuth2UserInfoFactory;

    private CustomOAuth2UserService customOAuth2UserService;

    @BeforeEach
    void setUp() {
        customOAuth2UserService = spy(new CustomOAuth2UserService(userService, oAuth2UserInfoFactory));
    }

    @Nested
    @DisplayName("loadUser")
    class LoadUser {

        @Test
        @DisplayName("성공 - UserService를 통해 사용자 조회/생성 후 CustomUserPrincipal 반환")
        void success_returnsCustomUserPrincipal() {
            // given
            OAuth2UserRequest request = createOAuth2UserRequest();
            OAuth2User kakaoOAuth2User = createKakaoOAuth2User();
            OAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(kakaoOAuth2User.getAttributes());

            doReturn(kakaoOAuth2User).when(customOAuth2UserService).fetchOAuth2User(request);
            given(oAuth2UserInfoFactory.create(eq("kakao"), anyMap())).willReturn(userInfo);

            User user = createUser(1L);
            given(userService.findOrCreateByOAuth(
                    eq("KAKAO"),
                    eq("12345"),
                    eq("test@kakao.com"),
                    eq("테스트유저"),
                    eq("https://example.com/image.png")
            )).willReturn(user);

            // when
            OAuth2User result = customOAuth2UserService.loadUser(request);

            // then
            verify(userService).findOrCreateByOAuth(
                    "KAKAO", "12345", "test@kakao.com", "테스트유저", "https://example.com/image.png"
            );
            assertThat(result).isInstanceOf(CustomUserPrincipal.class);
            CustomUserPrincipal principal = (CustomUserPrincipal) result;
            assertThat(principal.getUserId()).isEqualTo(1L);
            assertThat(principal.getEmail()).isEqualTo("test@kakao.com");
        }

        @Test
        @DisplayName("성공 - OAuth2User에 올바른 권한 설정")
        void success_setsCorrectAuthority() {
            // given
            OAuth2UserRequest request = createOAuth2UserRequest();
            OAuth2User kakaoOAuth2User = createKakaoOAuth2User();
            OAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(kakaoOAuth2User.getAttributes());

            doReturn(kakaoOAuth2User).when(customOAuth2UserService).fetchOAuth2User(request);
            given(oAuth2UserInfoFactory.create(eq("kakao"), anyMap())).willReturn(userInfo);

            User user = createUser(1L);
            given(userService.findOrCreateByOAuth(anyString(), anyString(), anyString(), anyString(), anyString()))
                    .willReturn(user);

            // when
            OAuth2User result = customOAuth2UserService.loadUser(request);

            // then
            assertThat(result.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("실패 - providerId가 없으면 OAuth2AuthenticationException 발생")
        void fail_whenProviderIdMissing() {
            // given
            OAuth2UserRequest request = createOAuth2UserRequest();
            OAuth2User invalidOAuth2User = createOAuth2UserWithoutKakaoAccount();

            doReturn(invalidOAuth2User).when(customOAuth2UserService).fetchOAuth2User(request);

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

            verify(userService, never()).findOrCreateByOAuth(anyString(), anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("실패 - 이메일이 없으면 OAuth2AuthenticationException 발생")
        void fail_whenEmailMissing() {
            // given
            OAuth2UserRequest request = createOAuth2UserRequest();
            OAuth2User invalidOAuth2User = createOAuth2UserWithoutEmail();

            doReturn(invalidOAuth2User).when(customOAuth2UserService).fetchOAuth2User(request);

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

            verify(userService, never()).findOrCreateByOAuth(anyString(), anyString(), anyString(), anyString(), anyString());
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
                .name("테스트유저")
                .provider("KAKAO")
                .providerId("12345")
                .profileImageUrl("https://example.com/image.png")
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
