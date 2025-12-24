package com.ipzy.domain.quiz.service;

import com.ipzy._global.common.enums.QuizType;
import com.ipzy.domain.quiz.dto.QuizAnswerRequest;
import com.ipzy.domain.quiz.dto.QuizAnswerResponse;
import com.ipzy.domain.quiz.dto.QuizQuestionResponse;
import com.ipzy.domain.quiz.dto.QuizSessionStartResponse;
import com.ipzy.domain.quiz.entity.Quiz;
import com.ipzy.domain.quiz.entity.QuizAnswer;
import com.ipzy.domain.quiz.entity.QuizOption;
import com.ipzy.domain.quiz.entity.QuizQuestion;
import com.ipzy.domain.quiz.entity.QuizSession;
import com.ipzy.domain.quiz.exception.QuizErrorCode;
import com.ipzy.domain.quiz.exception.QuizException;
import com.ipzy.domain.quiz.repository.QuizAnswerRepository;
import com.ipzy.domain.quiz.repository.QuizQuestionRepository;
import com.ipzy.domain.quiz.repository.QuizRepository;
import com.ipzy.domain.quiz.repository.QuizSessionRepository;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    QuizRepository quizRepository;

    @Mock
    QuizSessionRepository quizSessionRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    QuizQuestionRepository quizQuestionRepository;

    @Mock
    QuizAnswerRepository quizAnswerRepository;

    @InjectMocks
    QuizService quizService;

    private Quiz activeQuiz;
    private User user;
    private QuizQuestion requiredSingleQuestion;
    private QuizQuestion optionalMultipleQuestion;
    private QuizOption singleOption1;
    private QuizOption singleOption2;
    private QuizOption multiOption1;
    private QuizOption multiOption2;
    private QuizSession session;

    @BeforeEach
    void setUp() {
        activeQuiz = Quiz.builder()
                .title("테스트 퀴즈")
                .description("테스트용 퀴즈입니다")
                .isActive(true)
                .displayOrder(1)
                .build();

        user = User.builder()
                .email("test@ipzy.com")
                .name("tester")
                .phone("010-0000-0000")
                .provider("KAKAO")
                .providerId("12345")
                .profileImageUrl("https://example.com/profile.jpg")
                .password(null)
                .role(null)
                .status(null)
                .preferences(null)
                .build();

        // 필수 단일 선택 질문
        requiredSingleQuestion = QuizQuestion.builder()
                .quiz(activeQuiz)
                .text("당신의 스타일은?")
                .type(QuizType.SINGLE)
                .displayOrder(1)
                .required(true)
                .build();

        singleOption1 = QuizOption.builder()
                .question(requiredSingleQuestion)
                .text("미니멀")
                .value("minimal")
                .imageUrl(null)
                .displayOrder(1)
                .build();

        singleOption2 = QuizOption.builder()
                .question(requiredSingleQuestion)
                .text("스트릿")
                .value("street")
                .imageUrl(null)
                .displayOrder(2)
                .build();

        requiredSingleQuestion.addOption(singleOption1);
        requiredSingleQuestion.addOption(singleOption2);

        // 선택(필수 아님) 다중 선택 질문
        optionalMultipleQuestion = QuizQuestion.builder()
                .quiz(activeQuiz)
                .text("선호하는 색상은?")
                .type(QuizType.MULTIPLE)
                .displayOrder(2)
                .required(false)
                .build();

        multiOption1 = QuizOption.builder()
                .question(optionalMultipleQuestion)
                .text("블랙")
                .value("black")
                .imageUrl(null)
                .displayOrder(1)
                .build();

        multiOption2 = QuizOption.builder()
                .question(optionalMultipleQuestion)
                .text("화이트")
                .value("white")
                .imageUrl(null)
                .displayOrder(2)
                .build();

        optionalMultipleQuestion.addOption(multiOption1);
        optionalMultipleQuestion.addOption(multiOption2);

        // ID 설정
        ReflectionTestUtils.setField(activeQuiz, "id", 1L);
        ReflectionTestUtils.setField(requiredSingleQuestion, "id", 1L);
        ReflectionTestUtils.setField(optionalMultipleQuestion, "id", 2L);
        ReflectionTestUtils.setField(user, "id", 10L);

        session = QuizSession.builder()
                .quiz(activeQuiz)
                .user(user)
                .build();
        ReflectionTestUtils.setField(session, "id", 1L);
    }

    @Nested
    @DisplayName("getActiveQuizzes()")
    class GetActiveQuizzes {

        @Test
        @DisplayName("성공 - 활성화된 퀴즈 목록을 반환한다")
        void success() {
            // given
            Quiz quiz1 = Quiz.builder()
                    .title("퀴즈 1")
                    .description("설명 1")
                    .isActive(true)
                    .displayOrder(1)
                    .build();
            ReflectionTestUtils.setField(quiz1, "id", 1L);

            Quiz quiz2 = Quiz.builder()
                    .title("퀴즈 2")
                    .description("설명 2")
                    .isActive(true)
                    .displayOrder(2)
                    .build();
            ReflectionTestUtils.setField(quiz2, "id", 2L);

            given(quizRepository.findByIsActiveTrueOrderByDisplayOrderAsc())
                    .willReturn(List.of(quiz1, quiz2));

            // when
            var result = quizService.getActiveQuizzes();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getQuizId()).isEqualTo(1L);
            assertThat(result.get(1).getQuizId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("성공 - 활성화된 퀴즈가 없으면 빈 리스트를 반환한다")
        void success_emptyList() {
            // given
            given(quizRepository.findByIsActiveTrueOrderByDisplayOrderAsc())
                    .willReturn(List.of());

            // when
            var result = quizService.getActiveQuizzes();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("startQuiz()")
    class StartQuiz {

        @Test
        @DisplayName("성공 - 활성화된 퀴즈로 세션 생성 (로그인 사용자)")
        void success_withUser() {
            // given
            Long quizId = 1L;
            Long userId = 10L;
            given(quizRepository.findByIdAndIsActiveTrue(quizId)).willReturn(Optional.of(activeQuiz));
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            QuizSessionStartResponse response = quizService.startQuiz(quizId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getQuizId()).isEqualTo(activeQuiz.getId());
            assertThat(response.getUserId()).isEqualTo(user.getId());
            assertThat(response.getCompleted()).isFalse();
            verify(quizSessionRepository).save(any(QuizSession.class));
        }

        @Test
        @DisplayName("성공 - 활성화된 퀴즈로 세션 생성 (비로그인 사용자)")
        void success_withoutUser() {
            // given
            Long quizId = 1L;
            given(quizRepository.findByIdAndIsActiveTrue(quizId)).willReturn(Optional.of(activeQuiz));

            // when
            QuizSessionStartResponse response = quizService.startQuiz(quizId, null);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getQuizId()).isEqualTo(activeQuiz.getId());
            assertThat(response.getUserId()).isNull();
            verify(quizSessionRepository).save(any(QuizSession.class));
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("성공 - userId가 있지만 사용자가 없어도 세션 생성 가능")
        void success_userNotFoundButSessionCreated() {
            // given
            Long quizId = 1L;
            Long userId = 999L; // 존재하지 않는 사용자
            given(quizRepository.findByIdAndIsActiveTrue(quizId)).willReturn(Optional.of(activeQuiz));
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when
            QuizSessionStartResponse response = quizService.startQuiz(quizId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getQuizId()).isEqualTo(activeQuiz.getId());
            assertThat(response.getUserId()).isNull(); // 사용자가 없으면 null로 설정됨
            assertThat(response.getCompleted()).isFalse();
            verify(quizSessionRepository).save(any(QuizSession.class));
        }

        @Test
        @DisplayName("실패 - 비활성화된 퀴즈 또는 존재하지 않는 퀴즈면 QUIZ_NOT_FOUND 예외")
        void fail_quizNotFoundOrInactive() {
            // given
            Long quizId = 999L;
            given(quizRepository.findByIdAndIsActiveTrue(quizId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> quizService.startQuiz(quizId, null))
                    .isInstanceOf(QuizException.class)
                    .hasMessageContaining(QuizErrorCode.QUIZ_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("getQuestions()")
    class GetQuestions {

        @Test
        @DisplayName("성공 - 퀴즈의 모든 질문과 옵션을 반환한다")
        void success() {
            // given
            Long quizId = 1L;
            given(quizRepository.findByIdAndIsActiveTrue(quizId)).willReturn(Optional.of(activeQuiz));
            given(quizQuestionRepository.findAllByQuizIdWithOptions(quizId))
                    .willReturn(List.of(requiredSingleQuestion, optionalMultipleQuestion));

            // when
            List<QuizQuestionResponse> result = quizService.getQuestions(quizId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(QuizQuestionResponse::getQuestionId)
                    .containsExactly(requiredSingleQuestion.getId(), optionalMultipleQuestion.getId());
            assertThat(result.get(0).getOptions()).hasSize(2);
        }

        @Test
        @DisplayName("실패 - 비활성화된 퀴즈 또는 존재하지 않는 퀴즈면 QUIZ_NOT_FOUND 예외")
        void fail_quizNotFoundOrInactive() {
            // given
            Long quizId = 999L;
            given(quizRepository.findByIdAndIsActiveTrue(quizId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> quizService.getQuestions(quizId))
                    .isInstanceOf(QuizException.class)
                    .hasMessageContaining(QuizErrorCode.QUIZ_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("completeSession()")
    class CompleteSession {

        @Test
        @DisplayName("성공 - 모든 필수 질문이 답변되면 완료 처리된다")
        void success() {
            // given
            Long sessionId = 1L;

            QuizSession sessionWithAnswers = QuizSession.builder()
                    .user(user)
                    .quiz(activeQuiz)
                    .build();
            ReflectionTestUtils.setField(sessionWithAnswers, "id", sessionId);
            
            QuizAnswer answer1 = QuizAnswer.builder()
                    .session(sessionWithAnswers)
                    .question(requiredSingleQuestion)
                    .selectedOptions(List.of("minimal"))
                    .build();
            sessionWithAnswers.addAnswer(answer1);

            given(quizSessionRepository.findByIdWithAnswers(sessionId))
                    .willReturn(Optional.of(sessionWithAnswers));
            given(quizQuestionRepository.findAllByQuizIdWithOptions(activeQuiz.getId()))
                    .willReturn(List.of(requiredSingleQuestion));

            // when
            var response = quizService.completeSession(sessionId);

            // then
            assertThat(response.getSessionId()).isEqualTo(sessionId);
            assertThat(response.getCompleted()).isTrue();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 세션이면 SESSION_NOT_FOUND 예외")
        void fail_sessionNotFound() {
            // given
            Long sessionId = 999L;
            given(quizSessionRepository.findByIdWithAnswers(sessionId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> quizService.completeSession(sessionId))
                    .isInstanceOf(QuizException.class)
                    .hasMessageContaining(QuizErrorCode.SESSION_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패 - 이미 완료된 세션이면 SESSION_ALREADY_COMPLETED 예외")
        void fail_alreadyCompleted() {
            // given
            Long sessionId = 1L;
            QuizSession completedSession = QuizSession.builder()
                    .user(user)
                    .quiz(activeQuiz)
                    .build();
            ReflectionTestUtils.setField(completedSession, "id", sessionId);
            completedSession.complete();

            given(quizSessionRepository.findByIdWithAnswers(sessionId))
                    .willReturn(Optional.of(completedSession));

            // when & then
            assertThatThrownBy(() -> quizService.completeSession(sessionId))
                    .isInstanceOf(QuizException.class)
                    .hasMessageContaining(QuizErrorCode.SESSION_ALREADY_COMPLETED.getMessage());
        }

        @Test
        @DisplayName("실패 - 필수 질문 미답변 시 QUIZ_REQUIRED_NOT_ANSWERED 예외")
        void fail_requiredQuestionNotAnswered() {
            // given
            Long sessionId = 1L;
            QuizSession sessionWithoutAnswers = QuizSession.builder()
                    .user(user)
                    .quiz(activeQuiz)
                    .build();
            ReflectionTestUtils.setField(sessionWithoutAnswers, "id", sessionId);

            given(quizSessionRepository.findByIdWithAnswers(sessionId))
                    .willReturn(Optional.of(sessionWithoutAnswers));
            given(quizQuestionRepository.findAllByQuizIdWithOptions(activeQuiz.getId()))
                    .willReturn(List.of(requiredSingleQuestion));

            // when & then
            assertThatThrownBy(() -> quizService.completeSession(sessionId))
                    .isInstanceOf(QuizException.class)
                    .hasMessageContaining(QuizErrorCode.QUIZ_REQUIRED_NOT_ANSWERED.getMessage());
        }
    }

    @Nested
    @DisplayName("saveOrUpdateAnswer()")
    class SaveOrUpdateAnswer {

        @Test
        @DisplayName("성공 - 새 답변을 저장한다")
        void success_saveNewAnswer() {
            // given
            Long sessionId = 1L;
            QuizAnswerRequest request = new QuizAnswerRequest(
                    requiredSingleQuestion.getId(),
                    List.of("minimal")
            );

            given(quizSessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));
            given(quizQuestionRepository.findById(requiredSingleQuestion.getId()))
                    .willReturn(Optional.of(requiredSingleQuestion));
            given(quizAnswerRepository.findBySessionIdAndQuestionId(sessionId, requiredSingleQuestion.getId()))
                    .willReturn(Optional.empty());

            // when
            QuizAnswerResponse response = quizService.saveOrUpdateAnswer(sessionId, request);

            // then
            assertThat(response.getQuestionId()).isEqualTo(requiredSingleQuestion.getId());
            assertThat(response.getSelectedOptions()).containsExactly("minimal");
            verify(quizAnswerRepository).save(any(QuizAnswer.class));
        }

        @Test
        @DisplayName("성공 - 기존 답변을 업데이트한다")
        void success_updateExistingAnswer() {
            // given
            Long sessionId = 1L;
            QuizAnswerRequest request = new QuizAnswerRequest(
                    requiredSingleQuestion.getId(),
                    List.of("street")
            );

            QuizAnswer existingAnswer = QuizAnswer.builder()
                    .session(session)
                    .question(requiredSingleQuestion)
                    .selectedOptions(List.of("minimal"))
                    .build();

            given(quizSessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));
            given(quizQuestionRepository.findById(requiredSingleQuestion.getId()))
                    .willReturn(Optional.of(requiredSingleQuestion));
            given(quizAnswerRepository.findBySessionIdAndQuestionId(sessionId, requiredSingleQuestion.getId()))
                    .willReturn(Optional.of(existingAnswer));

            // when
            QuizAnswerResponse response = quizService.saveOrUpdateAnswer(sessionId, request);

            // then
            assertThat(response.getQuestionId()).isEqualTo(requiredSingleQuestion.getId());
            assertThat(response.getSelectedOptions()).containsExactly("street");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 세션이면 SESSION_NOT_FOUND 예외")
        void fail_sessionNotFound() {
            // given
            Long sessionId = 999L;
            QuizAnswerRequest request = new QuizAnswerRequest(
                    requiredSingleQuestion.getId(),
                    List.of("minimal")
            );

            given(quizSessionRepository.findById(sessionId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> quizService.saveOrUpdateAnswer(sessionId, request))
                    .isInstanceOf(QuizException.class)
                    .hasMessageContaining(QuizErrorCode.SESSION_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패 - 이미 완료된 세션이면 SESSION_ALREADY_COMPLETED 예외")
        void fail_alreadyCompletedSession() {
            // given
            Long sessionId = 1L;
            QuizSession completedSession = QuizSession.builder()
                    .user(user)
                    .quiz(activeQuiz)
                    .build();
            ReflectionTestUtils.setField(completedSession, "id", sessionId);
            completedSession.complete();

            QuizAnswerRequest request = new QuizAnswerRequest(
                    requiredSingleQuestion.getId(),
                    List.of("minimal")
            );

            given(quizSessionRepository.findById(sessionId)).willReturn(Optional.of(completedSession));

            // when & then
            assertThatThrownBy(() -> quizService.saveOrUpdateAnswer(sessionId, request))
                    .isInstanceOf(QuizException.class)
                    .hasMessageContaining(QuizErrorCode.SESSION_ALREADY_COMPLETED.getMessage());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 질문이면 QUIZ_QUESTION_NOT_FOUND 예외")
        void fail_questionNotFound() {
            // given
            Long sessionId = 1L;
            Long questionId = 999L;
            QuizAnswerRequest request = new QuizAnswerRequest(
                    questionId,
                    List.of("minimal")
            );

            given(quizSessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));
            given(quizQuestionRepository.findById(questionId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> quizService.saveOrUpdateAnswer(sessionId, request))
                    .isInstanceOf(QuizException.class)
                    .hasMessageContaining(QuizErrorCode.QUIZ_QUESTION_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패 - 세션의 퀴즈에 속하지 않는 질문이면 QUIZ_QUESTION_NOT_IN_SESSION 예외")
        void fail_questionNotInSessionQuiz() {
            // given
            Long sessionId = 1L;

            Quiz otherQuiz = Quiz.builder()
                    .title("다른 퀴즈")
                    .description("다른 퀴즈입니다")
                    .isActive(true)
                    .displayOrder(1)
                    .build();
            ReflectionTestUtils.setField(otherQuiz, "id", 2L);

            QuizQuestion questionFromOtherQuiz = QuizQuestion.builder()
                    .quiz(otherQuiz)
                    .text("다른 퀴즈 질문")
                    .type(QuizType.SINGLE)
                    .displayOrder(1)
                    .required(true)
                    .build();
            ReflectionTestUtils.setField(questionFromOtherQuiz, "id", 999L);

            QuizAnswerRequest request = new QuizAnswerRequest(
                    questionFromOtherQuiz.getId(),
                    List.of("minimal")
            );

            given(quizSessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));
            given(quizQuestionRepository.findById(questionFromOtherQuiz.getId()))
                    .willReturn(Optional.of(questionFromOtherQuiz));

            // when & then
            assertThatThrownBy(() -> quizService.saveOrUpdateAnswer(sessionId, request))
                    .isInstanceOf(QuizException.class)
                    .hasMessageContaining(QuizErrorCode.QUIZ_QUESTION_NOT_IN_SESSION.getMessage());
        }

        @Test
        @DisplayName("실패 - SINGLE 타입에 여러 옵션을 선택하면 QUIZ_ANSWER_TOO_MANY_OPTIONS 예외")
        void fail_singleTypeWithMultipleOptions() {
            // given
            Long sessionId = 1L;
            QuizAnswerRequest request = new QuizAnswerRequest(
                    requiredSingleQuestion.getId(),
                    List.of("minimal", "street")
            );

            given(quizSessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));
            given(quizQuestionRepository.findById(requiredSingleQuestion.getId()))
                    .willReturn(Optional.of(requiredSingleQuestion));

            // when & then
            assertThatThrownBy(() -> quizService.saveOrUpdateAnswer(sessionId, request))
                    .isInstanceOf(QuizException.class)
                    .hasMessageContaining(QuizErrorCode.QUIZ_ANSWER_TOO_MANY_OPTIONS.getMessage());
        }

        @Test
        @DisplayName("실패 - 유효하지 않은 옵션이면 QUIZ_OPTION_INVALID 예외")
        void fail_invalidOption() {
            // given
            Long sessionId = 1L;
            QuizAnswerRequest request = new QuizAnswerRequest(
                    requiredSingleQuestion.getId(),
                    List.of("invalid_option")
            );

            given(quizSessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));
            given(quizQuestionRepository.findById(requiredSingleQuestion.getId()))
                    .willReturn(Optional.of(requiredSingleQuestion));

            // when & then
            assertThatThrownBy(() -> quizService.saveOrUpdateAnswer(sessionId, request))
                    .isInstanceOf(QuizException.class)
                    .hasMessageContaining(QuizErrorCode.QUIZ_OPTION_INVALID.getMessage());
        }

        @Test
        @DisplayName("실패 - MULTIPLE 타입에서 선택 옵션이 비어있으면 INVALID_QUIZ_RESPONSE 예외")
        void fail_multipleTypeWithEmptyOptions() {
            // given
            Long sessionId = 1L;
            QuizAnswerRequest request = new QuizAnswerRequest(
                    optionalMultipleQuestion.getId(),
                    List.of()
            );

            given(quizSessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));
            given(quizQuestionRepository.findById(optionalMultipleQuestion.getId()))
                    .willReturn(Optional.of(optionalMultipleQuestion));

            // when & then
            assertThatThrownBy(() -> quizService.saveOrUpdateAnswer(sessionId, request))
                    .isInstanceOf(QuizException.class)
                    .hasMessageContaining(QuizErrorCode.INVALID_QUIZ_RESPONSE.getMessage());
        }
    }

    @Nested
    @DisplayName("cleanupExpiredSessions()")
    class CleanupExpiredSessions {

        @Test
        @DisplayName("성공 - 만료된 미완료 세션을 삭제한다")
        void success() {
            // given
            Duration expiration = Duration.ofMinutes(30);

            QuizSession expiredSession1 = QuizSession.builder()
                    .quiz(activeQuiz)
                    .user(null)
                    .build();
            ReflectionTestUtils.setField(expiredSession1, "id", 1L);

            QuizSession expiredSession2 = QuizSession.builder()
                    .quiz(activeQuiz)
                    .user(user)
                    .build();
            ReflectionTestUtils.setField(expiredSession2, "id", 2L);

            // 서비스 코드에서 LocalDateTime.now().minus(expiration)으로 cutoff를 계산하므로
            // ArgumentMatchers.any()를 사용하여 어떤 cutoff 값이든 매칭되도록 함
            given(quizSessionRepository.findByCompletedFalseAndCreatedAtBefore(any(LocalDateTime.class)))
                    .willReturn(List.of(expiredSession1, expiredSession2));

            // when
            int deletedCount = quizService.cleanupExpiredSessions(expiration);

            // then
            assertThat(deletedCount).isEqualTo(2);
            verify(quizSessionRepository).deleteAll(List.of(expiredSession1, expiredSession2));
        }

        @Test
        @DisplayName("성공 - 만료된 세션이 없으면 0을 반환한다")
        void success_noExpiredSessions() {
            // given
            Duration expiration = Duration.ofMinutes(30);

            given(quizSessionRepository.findByCompletedFalseAndCreatedAtBefore(any(LocalDateTime.class)))
                    .willReturn(List.of());

            // when
            int deletedCount = quizService.cleanupExpiredSessions(expiration);

            // then
            assertThat(deletedCount).isEqualTo(0);
            verify(quizSessionRepository).findByCompletedFalseAndCreatedAtBefore(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("실패 - expiration이 null이면 QUIZ_CLEANUP_EXPIRATION_NULL 예외")
        void fail_expirationNull() {
            // when & then
            assertThatThrownBy(() -> quizService.cleanupExpiredSessions(null))
                    .isInstanceOf(QuizException.class)
                    .hasMessageContaining(QuizErrorCode.QUIZ_CLEANUP_EXPIRATION_NULL.getMessage());
        }
    }
}
