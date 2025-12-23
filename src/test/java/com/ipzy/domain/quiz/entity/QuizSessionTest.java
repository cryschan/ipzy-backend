package com.ipzy.domain.quiz.entity;

import com.ipzy.domain.quiz.exception.QuizErrorCode;
import com.ipzy.domain.quiz.exception.QuizException;
import com.ipzy.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("QuizSession 엔티티 테스트")
class QuizSessionTest {

    private Quiz quiz;
    private User user;
    private QuizSession session;

    @BeforeEach
    void setUp() {
        quiz = Quiz.builder()
                .title("테스트 퀴즈")
                .description("테스트용 퀴즈입니다")
                .isActive(true)
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(quiz, "id", 1L);

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
        ReflectionTestUtils.setField(user, "id", 10L);

        session = QuizSession.builder()
                .quiz(quiz)
                .user(null)  // 비로그인 세션
                .build();
        ReflectionTestUtils.setField(session, "id", 1L);
    }

    @Nested
    @DisplayName("complete()")
    class Complete {

        @Test
        @DisplayName("성공 - 세션을 완료 처리한다")
        void success() {
            // given
            assertThat(session.getCompleted()).isFalse();

            // when
            session.complete();

            // then
            assertThat(session.getCompleted()).isTrue();
        }

        @Test
        @DisplayName("성공 - 이미 완료된 세션을 다시 완료 처리해도 문제없다")
        void success_alreadyCompleted() {
            // given
            session.complete();
            assertThat(session.getCompleted()).isTrue();

            // when
            session.complete();

            // then
            assertThat(session.getCompleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("addAnswer()")
    class AddAnswer {

        @Test
        @DisplayName("성공 - 답변을 추가한다")
        void success() {
            // given
            QuizQuestion question = QuizQuestion.builder()
                    .quiz(quiz)
                    .text("테스트 질문")
                    .type(com.ipzy._global.common.enums.QuizType.SINGLE)
                    .displayOrder(1)
                    .required(true)
                    .build();
            ReflectionTestUtils.setField(question, "id", 1L);

            QuizAnswer answer = QuizAnswer.builder()
                    .session(null)  // 아직 세션에 연결되지 않음
                    .question(question)
                    .selectedOptions(List.of("option1"))
                    .build();

            assertThat(session.getAnswers()).isEmpty();

            // when
            session.addAnswer(answer);

            // then
            assertThat(session.getAnswers()).hasSize(1);
            assertThat(session.getAnswers().get(0)).isEqualTo(answer);
            assertThat(answer.getSession()).isEqualTo(session);
        }

        @Test
        @DisplayName("성공 - 여러 답변을 추가할 수 있다")
        void success_multipleAnswers() {
            // given
            QuizQuestion question1 = QuizQuestion.builder()
                    .quiz(quiz)
                    .text("질문 1")
                    .type(com.ipzy._global.common.enums.QuizType.SINGLE)
                    .displayOrder(1)
                    .required(true)
                    .build();
            ReflectionTestUtils.setField(question1, "id", 1L);

            QuizQuestion question2 = QuizQuestion.builder()
                    .quiz(quiz)
                    .text("질문 2")
                    .type(com.ipzy._global.common.enums.QuizType.SINGLE)
                    .displayOrder(2)
                    .required(true)
                    .build();
            ReflectionTestUtils.setField(question2, "id", 2L);

            QuizAnswer answer1 = QuizAnswer.builder()
                    .session(null)
                    .question(question1)
                    .selectedOptions(List.of("option1"))
                    .build();

            QuizAnswer answer2 = QuizAnswer.builder()
                    .session(null)
                    .question(question2)
                    .selectedOptions(List.of("option2"))
                    .build();

            // when
            session.addAnswer(answer1);
            session.addAnswer(answer2);

            // then
            assertThat(session.getAnswers()).hasSize(2);
            assertThat(answer1.getSession()).isEqualTo(session);
            assertThat(answer2.getSession()).isEqualTo(session);
        }
    }

    @Nested
    @DisplayName("isAnonymous()")
    class IsAnonymous {

        @Test
        @DisplayName("성공 - user가 null이면 true를 반환한다")
        void success_userIsNull() {
            // given
            QuizSession anonymousSession = QuizSession.builder()
                    .quiz(quiz)
                    .user(null)
                    .build();

            // when & then
            assertThat(anonymousSession.isAnonymous()).isTrue();
        }

        @Test
        @DisplayName("성공 - user가 있으면 false를 반환한다")
        void success_userExists() {
            // given
            QuizSession loggedInSession = QuizSession.builder()
                    .quiz(quiz)
                    .user(user)
                    .build();

            // when & then
            assertThat(loggedInSession.isAnonymous()).isFalse();
        }
    }

    @Nested
    @DisplayName("assignUser()")
    class AssignUser {

        @Test
        @DisplayName("성공 - 익명 세션에 사용자를 연결한다")
        void success() {
            // given
            QuizSession anonymousSession = QuizSession.builder()
                    .quiz(quiz)
                    .user(null)
                    .build();
            assertThat(anonymousSession.isAnonymous()).isTrue();

            // when
            anonymousSession.assignUser(user);

            // then
            assertThat(anonymousSession.getUser()).isEqualTo(user);
            assertThat(anonymousSession.isAnonymous()).isFalse();
        }

        @Test
        @DisplayName("실패 - 이미 사용자가 연결된 세션에 사용자를 연결하면 QUIZ_005 예외")
        void fail_alreadyAssigned() {
            // given
            QuizSession assignedSession = QuizSession.builder()
                    .quiz(quiz)
                    .user(user)
                    .build();
            assertThat(assignedSession.getUser()).isEqualTo(user);

            User newUser = User.builder()
                    .email("new@ipzy.com")
                    .name("newuser")
                    .phone("010-1111-1111")
                    .provider("KAKAO")
                    .providerId("67890")
                    .profileImageUrl(null)
                    .password(null)
                    .role(null)
                    .status(null)
                    .preferences(null)
                    .build();
            ReflectionTestUtils.setField(newUser, "id", 20L);

            // when & then
            assertThatThrownBy(() -> assignedSession.assignUser(newUser))
                    .isInstanceOf(QuizException.class)
                    .hasMessageContaining(QuizErrorCode.SESSION_ALREADY_ASSIGNED.getMessage());
            
            // 기존 사용자가 유지되는지 확인
            assertThat(assignedSession.getUser()).isEqualTo(user);
        }
    }
}

