package com.ipzy.domain.quiz.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuizAnswer 엔티티 테스트")
class QuizAnswerTest {

    private Quiz quiz;
    private QuizSession session;
    private QuizQuestion question;
    private QuizAnswer answer;

    @BeforeEach
    void setUp() {
        quiz = Quiz.builder()
                .title("테스트 퀴즈")
                .description("테스트용 퀴즈입니다")
                .isActive(true)
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(quiz, "id", 1L);

        session = QuizSession.builder()
                .quiz(quiz)
                .user(null)
                .build();
        ReflectionTestUtils.setField(session, "id", 1L);

        question = QuizQuestion.builder()
                .quiz(quiz)
                .text("테스트 질문")
                .type(com.ipzy._global.common.enums.QuizType.SINGLE)
                .displayOrder(1)
                .required(true)
                .build();
        ReflectionTestUtils.setField(question, "id", 1L);

        answer = QuizAnswer.builder()
                .session(session)
                .question(question)
                .selectedOptions(List.of("option1"))
                .build();
        ReflectionTestUtils.setField(answer, "id", 1L);
    }

    @Nested
    @DisplayName("updateSelectedOptions()")
    class UpdateSelectedOptions {

        @Test
        @DisplayName("성공 - 선택한 옵션을 업데이트한다")
        void success() {
            // given
            List<String> originalOptions = answer.getSelectedOptions();
            assertThat(originalOptions).containsExactly("option1");

            List<String> newOptions = List.of("option2", "option3");

            // when
            answer.updateSelectedOptions(newOptions);

            // then
            assertThat(answer.getSelectedOptions()).containsExactly("option2", "option3");
            // 원본 리스트가 변경되지 않았는지 확인 (방어적 복사)
            assertThat(newOptions).containsExactly("option2", "option3");
        }

        @Test
        @DisplayName("성공 - null을 전달하면 빈 리스트로 설정된다")
        void success_nullBecomesEmptyList() {
            // given
            answer.updateSelectedOptions(List.of("option1"));
            assertThat(answer.getSelectedOptions()).isNotEmpty();

            // when
            answer.updateSelectedOptions(null);

            // then
            assertThat(answer.getSelectedOptions()).isEmpty();
        }

        @Test
        @DisplayName("성공 - 빈 리스트로 업데이트할 수 있다")
        void success_emptyList() {
            // given
            answer.updateSelectedOptions(List.of("option1"));
            assertThat(answer.getSelectedOptions()).isNotEmpty();

            // when
            answer.updateSelectedOptions(new ArrayList<>());

            // then
            assertThat(answer.getSelectedOptions()).isEmpty();
        }

        @Test
        @DisplayName("성공 - 같은 값으로 여러 번 업데이트해도 문제없다")
        void success_multipleUpdates() {
            // given
            List<String> options = List.of("option1", "option2");

            // when
            answer.updateSelectedOptions(options);
            answer.updateSelectedOptions(options);
            answer.updateSelectedOptions(options);

            // then
            assertThat(answer.getSelectedOptions()).containsExactly("option1", "option2");
        }

        @Test
        @DisplayName("성공 - 원본 리스트를 변경해도 내부 리스트는 변경되지 않는다 (방어적 복사)")
        void success_defensiveCopy() {
            // given
            List<String> mutableOptions = new ArrayList<>(List.of("option1", "option2"));
            answer.updateSelectedOptions(mutableOptions);

            // when
            mutableOptions.add("option3");  // 원본 리스트 수정

            // then
            assertThat(answer.getSelectedOptions()).containsExactly("option1", "option2");
            assertThat(answer.getSelectedOptions()).doesNotContain("option3");
        }
    }
}

