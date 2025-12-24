package com.ipzy.domain.quiz.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipzy._global.config.SecurityConfig;
import com.ipzy.domain.auth.handler.OAuth2FailureHandler;
import com.ipzy.domain.auth.handler.OAuth2LogoutSuccessHandler;
import com.ipzy.domain.auth.handler.OAuth2SuccessHandler;
import com.ipzy.domain.auth.service.CustomOAuth2UserService;
import com.ipzy.domain.quiz.dto.QuizAnswerRequest;
import com.ipzy.domain.quiz.dto.QuizAnswerResponse;
import com.ipzy.domain.quiz.dto.QuizCompletionResponse;
import com.ipzy.domain.quiz.exception.QuizErrorCode;
import com.ipzy.domain.quiz.exception.QuizException;
import com.ipzy.domain.quiz.service.QuizService;
import com.ipzy.domain.recommendation.service.RecommendationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizSessionController.class)
@Import({SecurityConfig.class, com.ipzy._global.exception.GlobalExceptionHandler.class})
@TestPropertySource(properties = {
    "spring.security.oauth2.client.registration.kakao.client-id=test-client-id",
    "app.oauth2.logout-redirect-uri=http://localhost:5173",
    "app.oauth2.success-redirect-uri=http://localhost:5173"
})
@DisplayName("QuizSessionController 테스트")
public class QuizSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QuizService quizService;

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

    @MockBean
    private org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private org.springframework.security.oauth2.client.OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @Nested
    @DisplayName("POST /api/quiz-sessions/{sessionId}/complete")
    class Complete {

        @Test
        @DisplayName("성공 - 세션 완료 처리 (비로그인 사용자)")
        void success_withAnonymousUser() throws Exception {
            // given
            Long sessionId = 1L;
            LocalDateTime completedAt = LocalDateTime.now();
            QuizCompletionResponse response = new QuizCompletionResponse(
                    sessionId,
                    true,  // completed
                    completedAt
            );

            given(quizService.completeSession(org.mockito.ArgumentMatchers.eq(sessionId)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/quiz-sessions/{sessionId}/complete", sessionId)
                            .param("autoGenerate", "false")
                            .with(anonymous())
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                    .andExpect(jsonPath("$.data.completed").value(true))
                    .andExpect(jsonPath("$.data.completedAt").exists());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 세션이면 404 (QUIZ_003)")
        void fail_sessionNotFound() throws Exception {
            // given
            Long sessionId = 999L;
            given(quizService.completeSession(org.mockito.ArgumentMatchers.eq(sessionId)))
                    .willThrow(new QuizException(QuizErrorCode.SESSION_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/quiz-sessions/{sessionId}/complete", sessionId)
                            .param("autoGenerate", "false")
                            .with(anonymous())
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("QUIZ_003"))
                    .andExpect(jsonPath("$.error.message").value("퀴즈 세션을 찾을 수 없습니다"));
        }

        @Test
        @DisplayName("실패 - 이미 완료된 세션이면 400 (QUIZ_004)")
        void fail_alreadyCompleted() throws Exception {
            // given
            Long sessionId = 1L;
            given(quizService.completeSession(org.mockito.ArgumentMatchers.eq(sessionId)))
                    .willThrow(new QuizException(QuizErrorCode.SESSION_ALREADY_COMPLETED));

            // when & then
            mockMvc.perform(post("/api/quiz-sessions/{sessionId}/complete", sessionId)
                            .param("autoGenerate", "false")
                            .with(anonymous())
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("QUIZ_004"))
                    .andExpect(jsonPath("$.error.message").value("이미 완료된 퀴즈 세션입니다"));
        }

        @Test
        @DisplayName("실패 - 필수 질문 미답변 시 400 (QUIZ_008)")
        void fail_requiredQuestionNotAnswered() throws Exception {
            // given
            Long sessionId = 1L;
            given(quizService.completeSession(org.mockito.ArgumentMatchers.eq(sessionId)))
                    .willThrow(new QuizException(QuizErrorCode.QUIZ_REQUIRED_NOT_ANSWERED));

            // when & then
            mockMvc.perform(post("/api/quiz-sessions/{sessionId}/complete", sessionId)
                            .param("autoGenerate", "false")
                            .with(anonymous())
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("QUIZ_008"))
                    .andExpect(jsonPath("$.error.message").value("필수 질문에 답변하지 않았습니다"));
        }
    }

    @Nested
    @DisplayName("POST /api/quiz-sessions/{sessionId}/answers")
    class SaveAnswer {

        @Test
        @DisplayName("성공 - 새 답변 저장 (비로그인 사용자)")
        void success_saveNewAnswer() throws Exception {
            // given
            Long sessionId = 1L;
            QuizAnswerRequest request = new QuizAnswerRequest(1L, List.of("clean"));
            QuizAnswerResponse response = new QuizAnswerResponse(1L, List.of("clean"));

            given(quizService.saveOrUpdateAnswer(org.mockito.ArgumentMatchers.eq(sessionId), org.mockito.ArgumentMatchers.any(QuizAnswerRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/quiz-sessions/{sessionId}/answers", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(anonymous())
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.questionId").value(1))
                    .andExpect(jsonPath("$.data.selectedOptions").isArray())
                    .andExpect(jsonPath("$.data.selectedOptions[0]").value("clean"));
        }

        @Test
        @DisplayName("성공 - 기존 답변 수정 (비로그인 사용자)")
        void success_updateExistingAnswer() throws Exception {
            // given
            Long sessionId = 1L;
            QuizAnswerRequest request = new QuizAnswerRequest(1L, List.of("street"));
            QuizAnswerResponse response = new QuizAnswerResponse(1L, List.of("street"));

            given(quizService.saveOrUpdateAnswer(org.mockito.ArgumentMatchers.eq(sessionId), org.mockito.ArgumentMatchers.any(QuizAnswerRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/quiz-sessions/{sessionId}/answers", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(anonymous())
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.questionId").value(1))
                    .andExpect(jsonPath("$.data.selectedOptions[0]").value("street"));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 세션이면 404 (QUIZ_003)")
        void fail_sessionNotFound() throws Exception {
            // given
            Long sessionId = 999L;
            QuizAnswerRequest request = new QuizAnswerRequest(1L, List.of("clean"));

            given(quizService.saveOrUpdateAnswer(org.mockito.ArgumentMatchers.eq(sessionId), org.mockito.ArgumentMatchers.any(QuizAnswerRequest.class)))
                    .willThrow(new QuizException(QuizErrorCode.SESSION_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/quiz-sessions/{sessionId}/answers", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(anonymous())
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("QUIZ_003"))
                    .andExpect(jsonPath("$.error.message").value("퀴즈 세션을 찾을 수 없습니다"));
        }

        @Test
        @DisplayName("실패 - 이미 완료된 세션이면 400 (QUIZ_004)")
        void fail_alreadyCompletedSession() throws Exception {
            // given
            Long sessionId = 1L;
            QuizAnswerRequest request = new QuizAnswerRequest(1L, List.of("clean"));

            given(quizService.saveOrUpdateAnswer(org.mockito.ArgumentMatchers.eq(sessionId), org.mockito.ArgumentMatchers.any(QuizAnswerRequest.class)))
                    .willThrow(new QuizException(QuizErrorCode.SESSION_ALREADY_COMPLETED));

            // when & then
            mockMvc.perform(post("/api/quiz-sessions/{sessionId}/answers", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(anonymous())
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("QUIZ_004"))
                    .andExpect(jsonPath("$.error.message").value("이미 완료된 퀴즈 세션입니다"));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 질문이면 404 (QUIZ_006)")
        void fail_questionNotFound() throws Exception {
            // given
            Long sessionId = 1L;
            QuizAnswerRequest request = new QuizAnswerRequest(999L, List.of("clean"));

            given(quizService.saveOrUpdateAnswer(org.mockito.ArgumentMatchers.eq(sessionId), org.mockito.ArgumentMatchers.any(QuizAnswerRequest.class)))
                    .willThrow(new QuizException(QuizErrorCode.QUIZ_QUESTION_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/quiz-sessions/{sessionId}/answers", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(anonymous())
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("QUIZ_006"))
                    .andExpect(jsonPath("$.error.message").value("퀴즈 질문을 찾을 수 없습니다"));
        }

        @Test
        @DisplayName("실패 - 질문이 세션의 퀴즈에 속하지 않으면 400 (QUIZ_007)")
        void fail_questionNotInSessionQuiz() throws Exception {
            // given
            Long sessionId = 1L;
            QuizAnswerRequest request = new QuizAnswerRequest(999L, List.of("clean"));

            given(quizService.saveOrUpdateAnswer(org.mockito.ArgumentMatchers.eq(sessionId), org.mockito.ArgumentMatchers.any(QuizAnswerRequest.class)))
                    .willThrow(new QuizException(QuizErrorCode.QUIZ_QUESTION_NOT_IN_SESSION));

            // when & then
            mockMvc.perform(post("/api/quiz-sessions/{sessionId}/answers", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(anonymous())
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("QUIZ_007"))
                    .andExpect(jsonPath("$.error.message").value("해당 질문이 세션의 퀴즈에 속하지 않습니다"));
        }

        @Test
        @DisplayName("실패 - SINGLE 타입에 여러 옵션 선택 시 400 (QUIZ_009)")
        void fail_singleTypeWithMultipleOptions() throws Exception {
            // given
            Long sessionId = 1L;
            QuizAnswerRequest request = new QuizAnswerRequest(1L, List.of("clean", "minimal"));

            given(quizService.saveOrUpdateAnswer(org.mockito.ArgumentMatchers.eq(sessionId), org.mockito.ArgumentMatchers.any(QuizAnswerRequest.class)))
                    .willThrow(new QuizException(QuizErrorCode.QUIZ_ANSWER_TOO_MANY_OPTIONS));

            // when & then
            mockMvc.perform(post("/api/quiz-sessions/{sessionId}/answers", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(anonymous())
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("QUIZ_009"))
                    .andExpect(jsonPath("$.error.message").value("단일 선택 질문에는 1개의 옵션만 선택할 수 있습니다"));
        }

        @Test
        @DisplayName("실패 - 유효하지 않은 옵션이면 400 (QUIZ_012)")
        void fail_invalidOption() throws Exception {
            // given
            Long sessionId = 1L;
            QuizAnswerRequest request = new QuizAnswerRequest(1L, List.of("invalid_option"));

            given(quizService.saveOrUpdateAnswer(org.mockito.ArgumentMatchers.eq(sessionId), org.mockito.ArgumentMatchers.any(QuizAnswerRequest.class)))
                    .willThrow(new QuizException(QuizErrorCode.QUIZ_OPTION_INVALID));

            // when & then
            mockMvc.perform(post("/api/quiz-sessions/{sessionId}/answers", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(anonymous())
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("QUIZ_012"))
                    .andExpect(jsonPath("$.error.message").value("유효하지 않은 옵션입니다"));
        }

        @Test
        @DisplayName("실패 - 요청 본문이 유효하지 않으면 400 (validation error)")
        void fail_invalidRequest() throws Exception {
            // given
            Long sessionId = 1L;
            // questionId가 null인 잘못된 요청
            String invalidJson = "{\"questionId\": null, \"selectedOptions\": []}";

            // when & then
            mockMvc.perform(post("/api/quiz-sessions/{sessionId}/answers", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson)
                            .with(anonymous())
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }
}

