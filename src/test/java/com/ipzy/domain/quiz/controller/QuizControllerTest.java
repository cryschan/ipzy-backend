package com.ipzy.domain.quiz.controller;

import com.ipzy._global.common.enums.UserRole;
import com.ipzy._global.config.SecurityConfig;
import com.ipzy.domain.auth.dto.CustomUserPrincipal;
import com.ipzy.domain.auth.handler.OAuth2FailureHandler;
import com.ipzy.domain.auth.handler.OAuth2LogoutSuccessHandler;
import com.ipzy.domain.auth.handler.OAuth2SuccessHandler;
import com.ipzy.domain.auth.service.CustomOAuth2UserService;
import com.ipzy.domain.quiz.dto.QuizListResponse;
import com.ipzy.domain.quiz.dto.QuizQuestionResponse;
import com.ipzy.domain.quiz.dto.QuizSessionStartResponse;
import com.ipzy.domain.quiz.exception.QuizErrorCode;
import com.ipzy.domain.quiz.exception.QuizException;
import com.ipzy.domain.quiz.service.QuizService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizController.class)
@Import({SecurityConfig.class, com.ipzy._global.exception.GlobalExceptionHandler.class})
@TestPropertySource(properties = {
    "spring.security.oauth2.client.registration.kakao.client-id=test-client-id",
    "app.oauth2.logout-redirect-uri=http://localhost:5173",
    "app.oauth2.success-redirect-uri=http://localhost:5173"
})
@DisplayName("QuizController 테스트")
public class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuizService quizService;

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

    /**
     * 테스트용 CustomUserPrincipal 생성
     */
    private CustomUserPrincipal createTestPrincipal() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", 1L);
        attributes.put("id", 12345L);  // OAuth2 provider id

        return CustomUserPrincipal.builder()
                .userId(1L)
                .email("test@example.com")
                .userName("testuser")
                .profileImageUrl(null)
                .role(UserRole.USER)
                .attributes(attributes)
                .build();
    }

    @Nested
    @DisplayName("GET /api/quizzes")
    class GetQuizzes {

        @Test
        @DisplayName("성공 - 활성화된 퀴즈 목록 반환 (비로그인 접근 가능)")
        void success() throws Exception {
            // given
            QuizListResponse quiz1 = new QuizListResponse(1L, "스타일 퀴즈", "나만의 스타일을 찾아보세요", 1);
            QuizListResponse quiz2 = new QuizListResponse(2L, "계절별 코디 퀴즈", "계절에 맞는 코디를 추천받아보세요", 2);

            given(quizService.getActiveQuizzes())
                    .willReturn(List.of(quiz1, quiz2));

            // when & then
            mockMvc.perform(get("/api/quizzes"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].quizId").value(1))
                    .andExpect(jsonPath("$.data[0].title").value("스타일 퀴즈"))
                    .andExpect(jsonPath("$.data[0].description").value("나만의 스타일을 찾아보세요"))
                    .andExpect(jsonPath("$.data[0].displayOrder").value(1))
                    .andExpect(jsonPath("$.data[1].quizId").value(2))
                    .andExpect(jsonPath("$.data[1].title").value("계절별 코디 퀴즈"));
        }

        @Test
        @DisplayName("성공 - 퀴즈가 없으면 빈 배열 반환")
        void success_emptyList() throws Exception {
            // given
            given(quizService.getActiveQuizzes())
                    .willReturn(List.of());

            //when & then
            mockMvc.perform(get("/api/quizzes"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

    }

    @Nested
    @DisplayName("POST /api/quizzes/{quizId}/sessions")
    class StartQuiz {

        @Test
        @DisplayName("성공 - 퀴즈 세션 시작 (로그인 사용자)")
        void success_withLoggedInUser() throws Exception {
            //given
            Long quizId = 1L;
            QuizSessionStartResponse response = new QuizSessionStartResponse(
                    1L,     // sessionId
                    1L,             // userId
                    quizId,
                    false,  // completed
                    LocalDateTime.now()     // createdAt
            );

            given(quizService.startQuiz(quizId, 1L))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/quizzes/{quizId}/sessions", quizId)
                    .with(oauth2Login().oauth2User(createTestPrincipal()))
                    .with(csrf()))
                    .andDo(result -> {
                        // 에러 발생 시 상세 정보 출력
                        if (result.getResponse().getStatus() != 200) {
                            System.err.println("=== ERROR DETAILS ===");
                            System.err.println("Status: " + result.getResponse().getStatus());
                            System.err.println("Response Body: " + result.getResponse().getContentAsString());
                            if (result.getResolvedException() != null) {
                                System.err.println("Exception: " + result.getResolvedException().getClass().getName());
                                System.err.println("Message: " + result.getResolvedException().getMessage());
                                result.getResolvedException().printStackTrace();
                            }
                        }
                    })
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.sessionId").value(1))
                    .andExpect(jsonPath("$.data.userId").value(1))
                    .andExpect(jsonPath("$.data.quizId").value(quizId))
                    .andExpect(jsonPath("$.data.completed").value(false));

        }

        @Test
        @DisplayName("성공 - 퀴즈 세션 시작 (비로그인 사용자)")
        void success_withAnonymousUser() throws Exception {
            // given
            Long quizId = 1L;
            QuizSessionStartResponse response = new QuizSessionStartResponse(
                    1L,  // sessionId
                    null,  // userId (비로그인)
                    quizId,
                    false,  // completed
                    LocalDateTime.now()  // createdAt
            );

            given(quizService.startQuiz(quizId, null))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/quizzes/{quizId}/sessions", quizId)
                            .with(csrf()))
                    .andDo(result -> {
                        // 에러 발생 시 상세 정보 출력
                        if (result.getResponse().getStatus() != 200) {
                            System.err.println("=== ERROR DETAILS (Anonymous User) ===");
                            System.err.println("Status: " + result.getResponse().getStatus());
                            System.err.println("Response Body: " + result.getResponse().getContentAsString());
                            if (result.getResolvedException() != null) {
                                System.err.println("Exception: " + result.getResolvedException().getClass().getName());
                                System.err.println("Message: " + result.getResolvedException().getMessage());
                                result.getResolvedException().printStackTrace();
                            }
                        }
                    })
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.sessionId").value(1))
                    .andExpect(jsonPath("$.data.userId").isEmpty())
                    .andExpect(jsonPath("$.data.quizId").value(quizId))
                    .andExpect(jsonPath("$.data.completed").value(false));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 퀴즈 (QUIZ_001")
        void fail_quizNotFound() throws Exception {
            // given
            Long quizId = 999L;
            given(quizService.startQuiz(quizId, null))
                    .willThrow(new QuizException(QuizErrorCode.QUIZ_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/quizzes/{quizId}/sessions", quizId)
                    .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("QUIZ_001"))
                    .andExpect(jsonPath("$.error.message").value("퀴즈를 찾을 수 없습니다"));
        }
    }

    @Nested
    @DisplayName("GET /api/quizzes/{quizId}/questions")
    class GetQuestions {

        @Test
        @DisplayName("성공 - 퀴즈 질문 목록 반환 (비로그인 접근 가능)")
        void success() throws Exception {
            // given
            Long quizId = 1L;

            QuizQuestionResponse question1 = QuizQuestionResponse.builder()
                    .questionId(1L)
                    .text("어떻게 보이고 싶어요?")
                    .type("SINGLE")
                    .required(true)
                    .displayOrder(1)
                    .options(List.of())
                    .build();

            QuizQuestionResponse question2 = QuizQuestionResponse.builder()
                    .questionId(2L)
                    .text("선호하는 스타일을 선택해주세요 (복수 선택 가능)")
                    .type("MULTIPLE")
                    .required(false)
                    .displayOrder(2)
                    .options(List.of())
                    .build();

            given(quizService.getQuestions(quizId))
                    .willReturn(List.of(question1, question2));

            // when & then
            mockMvc.perform(get("/api/quizzes/{quizId}/questions", quizId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].questionId").value(1))
                    .andExpect(jsonPath("$.data[0].text").value("어떻게 보이고 싶어요?"))
                    .andExpect(jsonPath("$.data[0].type").value("SINGLE"))
                    .andExpect(jsonPath("$.data[0].required").value(true))
                    .andExpect(jsonPath("$.data[0].displayOrder").value(1))
                    .andExpect(jsonPath("$.data[1].questionId").value(2))
                    .andExpect(jsonPath("$.data[1].text").value("선호하는 스타일을 선택해주세요 (복수 선택 가능)"))
                    .andExpect(jsonPath("$.data[1].type").value("MULTIPLE"))
                    .andExpect(jsonPath("$.data[1].required").value(false));
        }

        @Test
        @DisplayName("성공 - 질문이 없으면 빈 배열 반환")
        void success_emptyList() throws Exception {
            // given
            Long quizId = 1L;
            given(quizService.getQuestions(quizId))
                    .willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/quizzes/{quizId}/questions", quizId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 퀴즈 (QUIZ_001")
        void fail_quizNotFound() throws Exception {
            // given
            Long quizId = 999L;
            given(quizService.getQuestions(quizId))
                    .willThrow(new QuizException(QuizErrorCode.QUIZ_NOT_FOUND));


            // when & then
            mockMvc.perform(get("/api/quizzes/{quizId}/questions", quizId))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("QUIZ_001"))
                    .andExpect(jsonPath("$.error.message").value("퀴즈를 찾을 수 없습니다"));
        }

    }

}

