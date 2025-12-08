package com.ipzy.domain.auth.controller;

import com.ipzy.domain.auth.dto.CustomUserPrincipal;
import com.ipzy.domain.auth.handler.OAuth2FailureHandler;
import com.ipzy.domain.auth.handler.OAuth2LogoutSuccessHandler;
import com.ipzy.domain.auth.handler.OAuth2SuccessHandler;
import com.ipzy.domain.auth.logout.OAuth2LogoutStrategyFactory;
import com.ipzy.domain.auth.service.CustomOAuth2UserService;
import com.ipzy.global.common.enums.UserRole;
import com.ipzy.global.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.Cookie;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, OAuth2LogoutSuccessHandler.class})
@TestPropertySource(properties = {
    "spring.security.oauth2.client.registration.kakao.client-id=test-client-id",
    "app.oauth2.logout-redirect-uri=http://localhost:5173",
    "app.oauth2.success-redirect-uri=http://localhost:5173"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockBean
    private OAuth2FailureHandler oAuth2FailureHandler;

    @MockBean
    private OAuth2LogoutStrategyFactory oAuth2LogoutStrategyFactory;

    @Nested
    @DisplayName("GET /api/auth/me")
    class GetAuthMe {

        @Test
        @DisplayName("인증된 사용자 정보 반환")
        void getAuthenticatedUser() throws Exception {
            CustomUserPrincipal principal = createCustomUserPrincipal();

            mockMvc.perform(get("/api/auth/me")
                            .with(oauth2Login().oauth2User(principal)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(1))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.name").value("테스트"))
                    .andExpect(jsonPath("$.data.profileImageUrl").value("https://example.com/profile.jpg"));
        }

        @Test
        @DisplayName("비인증 사용자 요청 시 401 반환 (AUTH_001)")
        void getUnauthenticatedUser_returns401() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("AUTH_001"))
                    .andExpect(jsonPath("$.error.message").value("인증이 필요합니다"));
        }

        @Test
        @DisplayName("세션 만료 시 401 반환 (AUTH_005) - JSESSIONID 쿠키는 있지만 세션 없음")
        void getSessionExpiredUser_returns401WithSessionExpired() throws Exception {
            // JSESSIONID 쿠키가 있지만 실제 세션은 없는 상황 시뮬레이션
            Cookie expiredSessionCookie = new Cookie("JSESSIONID", "expired-session-id");

            mockMvc.perform(get("/api/auth/me")
                            .cookie(expiredSessionCookie))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("AUTH_005"))
                    .andExpect(jsonPath("$.error.message").value("세션이 만료되었습니다"));
        }

    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class PostLogout {

        @Test
        @DisplayName("로그아웃 성공 시 200 OK와 JSON 응답 반환")
        void logout_success() throws Exception {
            CustomUserPrincipal principal = createCustomUserPrincipal();

            mockMvc.perform(post("/api/auth/logout")
                            .with(oauth2Login().oauth2User(principal))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.message").value("로그아웃 되었습니다"));
        }

        @Test
        @DisplayName("비로그인 상태에서 로그아웃 요청 시에도 200 OK 반환")
        void logout_withoutLogin_returns200() throws Exception {
            mockMvc.perform(post("/api/auth/logout")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.message").value("로그아웃 되었습니다"));
        }

    }

    private CustomUserPrincipal createCustomUserPrincipal() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);

        return CustomUserPrincipal.builder()
                .userId(1L)
                .email("test@example.com")
                .userName("테스트")
                .profileImageUrl("https://example.com/profile.jpg")
                .role(UserRole.USER)
                .attributes(attributes)
                .build();
    }
}
