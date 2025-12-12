package com.ipzy.domain.recommendation.controller;

import com.ipzy._global.config.SecurityConfig;
import com.ipzy.domain.auth.handler.OAuth2FailureHandler;
import com.ipzy.domain.auth.handler.OAuth2LogoutSuccessHandler;
import com.ipzy.domain.auth.handler.OAuth2SuccessHandler;
import com.ipzy.domain.auth.service.CustomOAuth2UserService;
import com.ipzy.domain.recommendation.entity.Recommendation;
import com.ipzy.domain.recommendation.exception.RecommendationException;
import com.ipzy.domain.recommendation.service.RecommendationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.ipzy._global.common.enums.UserRole;
import com.ipzy.domain.auth.dto.CustomUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecommendationController.class)
@Import(SecurityConfig.class)
@DisplayName("RecommendationController 테스트")
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendationService recommendationService;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockBean
    private OAuth2FailureHandler oAuth2FailureHandler;

    @MockBean
    private OAuth2LogoutSuccessHandler oAuth2LogoutSuccessHandler;

    /**
     * 테스트용 CustomUserPrincipal 생성
     */
    private CustomUserPrincipal createTestPrincipal() {
        return CustomUserPrincipal.builder()
                .userId(1L)
                .email("test@example.com")
                .userName("testuser")
                .profileImageUrl(null)
                .role(UserRole.USER)
                .attributes(Map.of("userId", 1L))
                .build();
    }

    @Nested
    @DisplayName("GET /api/recommendations/test")
    class TestConnection {

        @Test
        @DisplayName("메시지 전송 시 성공 응답 반환")
        void testConnection_success() throws Exception {
            // given
            String message = "안녕하세요";
            String mockResponse = "[Mock] Python 응답: " + message;
            given(recommendationService.testConnection(anyString())).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/recommendations/test")
                            .param("msg", message)
                            .with(user("testuser")))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(mockResponse));
        }

        @Test
        @DisplayName("빈 메시지도 처리 가능")
        void testConnection_withEmptyMessage() throws Exception {
            // given
            String message = "";
            String mockResponse = "[Mock] Python 응답: ";
            given(recommendationService.testConnection(anyString())).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/recommendations/test")
                            .param("msg", message)
                            .with(user("testuser")))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

    }

    @Nested
    @DisplayName("POST /api/recommendations/sessions/{sessionId}/generate")
    class GenerateRecommendation {

        @Test
        @DisplayName("성공 - 추천 생성")
        void success() throws Exception {
            // Given
            Long sessionId = 100L;
            Recommendation recommendation = Recommendation.builder()
                    .displayOrder(1)
                    .occasion("데이트")
                    .season("봄")
                    .style("캐주얼")
                    .reason("밝은 색감의 캐주얼 룩입니다.")
                    .totalPrice(150000)
                    .build();
            ReflectionTestUtils.setField(recommendation, "id", 1L);

            given(recommendationService.generateRecommendation(anyLong(), any()))
                    .willReturn(List.of(recommendation));

            // When & Then
            mockMvc.perform(post("/api/recommendations/sessions/{sessionId}/generate", sessionId)
                            .with(oauth2Login().oauth2User(createTestPrincipal()))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].recommendationId").value(1))
                    .andExpect(jsonPath("$.data[0].occasion").value("데이트"))
                    .andExpect(jsonPath("$.data[0].style").value("캐주얼"));
        }

        @Test
        @DisplayName("실패 - 세션 없음")
        void fail_sessionNotFound() throws Exception {
            // Given
            Long sessionId = 999L;
            given(recommendationService.generateRecommendation(anyLong(), any()))
                    .willThrow(RecommendationException.sessionNotFound(sessionId));

            // When & Then
            mockMvc.perform(post("/api/recommendations/sessions/{sessionId}/generate", sessionId)
                            .with(oauth2Login().oauth2User(createTestPrincipal()))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 - 비로그인 사용자 401")
        void fail_unauthorized() throws Exception {
            // Given
            Long sessionId = 100L;

            // When & Then - 인증 없이 요청
            mockMvc.perform(post("/api/recommendations/sessions/{sessionId}/generate", sessionId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/recommendations/sessions/{sessionId}")
    class GetRecommendationsBySession {

        @Test
        @DisplayName("성공 - 세션별 추천 조회")
        void success() throws Exception {
            // Given
            Long sessionId = 100L;
            Recommendation recommendation = Recommendation.builder()
                    .displayOrder(1)
                    .occasion("데이트")
                    .season("봄")
                    .style("캐주얼")
                    .reason("밝은 색감의 캐주얼 룩입니다.")
                    .totalPrice(150000)
                    .build();
            ReflectionTestUtils.setField(recommendation, "id", 1L);

            given(recommendationService.getRecommendationsBySession(anyLong(), any()))
                    .willReturn(List.of(recommendation));

            // When & Then
            mockMvc.perform(get("/api/recommendations/sessions/{sessionId}", sessionId)
                            .with(user("testuser")))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].occasion").value("데이트"));
        }

        @Test
        @DisplayName("성공 - 추천 없는 경우 빈 배열 반환")
        void success_emptyResult() throws Exception {
            // Given
            Long sessionId = 100L;
            given(recommendationService.getRecommendationsBySession(anyLong(), any()))
                    .willReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/recommendations/sessions/{sessionId}", sessionId)
                            .with(user("testuser")))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }
}
