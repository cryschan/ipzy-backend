package com.ipzy.domain.recommendation.controller;

import com.ipzy.domain.auth.handler.OAuth2FailureHandler;
import com.ipzy.domain.auth.handler.OAuth2LogoutSuccessHandler;
import com.ipzy.domain.auth.handler.OAuth2SuccessHandler;
import com.ipzy.domain.auth.service.CustomOAuth2UserService;
import com.ipzy.domain.recommendation.client.PythonAiClient;
import com.ipzy._global.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private PythonAiClient pythonAiClient;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockBean
    private OAuth2FailureHandler oAuth2FailureHandler;

    @MockBean
    private OAuth2LogoutSuccessHandler oAuth2LogoutSuccessHandler;

    @Nested
    @DisplayName("GET /api/recommendations/test")
    class TestConnection {

        @Test
        @DisplayName("메시지 전송 시 성공 응답 반환")
        void testConnection_success() throws Exception {
            // given
            String message = "안녕하세요";
            String mockResponse = "Python 응답 (Mock): " + message;
            given(pythonAiClient.testConnection(anyString())).willReturn(mockResponse);

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
            String mockResponse = "Python 응답 (Mock): ";
            given(pythonAiClient.testConnection(anyString())).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/recommendations/test")
                            .param("msg", message)
                            .with(user("testuser")))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

    }
}
