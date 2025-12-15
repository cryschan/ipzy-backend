package com.ipzy.domain.recommendation.dto.request;

import com.ipzy.domain.quiz.entity.QuizAnswer;
import com.ipzy.domain.quiz.entity.QuizQuestion;
import com.ipzy.domain.quiz.entity.QuizSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("QuizAnswerDto 테스트")
class QuizAnswerDtoTest {

    @Nested
    @DisplayName("from 메서드")
    class From {

        @Test
        @DisplayName("성공 - QuizAnswer를 QuizAnswerDto로 변환한다")
        void success() {
            // Given
            QuizQuestion question = mock(QuizQuestion.class);
            when(question.getId()).thenReturn(1L);
            when(question.getText()).thenReturn("선호하는 스타일은?");

            QuizSession session = mock(QuizSession.class);
            QuizAnswer answer = QuizAnswer.builder()
                    .session(session)
                    .question(question)
                    .selectedOptions(List.of("캐주얼", "스트릿"))
                    .build();

            // When
            QuizAnswerDto dto = QuizAnswerDto.from(answer);

            // Then
            assertThat(dto.questionId()).isEqualTo(1L);
            assertThat(dto.questionText()).isEqualTo("선호하는 스타일은?");
            assertThat(dto.selectedOptions()).containsExactly("캐주얼", "스트릿");
        }

        @Test
        @DisplayName("성공 - 단일 선택 옵션도 정상 변환된다")
        void success_singleOption() {
            // Given
            QuizQuestion question = mock(QuizQuestion.class);
            when(question.getId()).thenReturn(2L);
            when(question.getText()).thenReturn("선호하는 색상은?");

            QuizSession session = mock(QuizSession.class);
            QuizAnswer answer = QuizAnswer.builder()
                    .session(session)
                    .question(question)
                    .selectedOptions(List.of("블랙"))
                    .build();

            // When
            QuizAnswerDto dto = QuizAnswerDto.from(answer);

            // Then
            assertThat(dto.questionId()).isEqualTo(2L);
            assertThat(dto.questionText()).isEqualTo("선호하는 색상은?");
            assertThat(dto.selectedOptions()).hasSize(1);
            assertThat(dto.selectedOptions()).containsExactly("블랙");
        }

        @Test
        @DisplayName("성공 - 빈 선택 옵션도 정상 처리된다")
        void success_emptyOptions() {
            // Given
            QuizQuestion question = mock(QuizQuestion.class);
            when(question.getId()).thenReturn(3L);
            when(question.getText()).thenReturn("추가 의견이 있나요?");

            QuizSession session = mock(QuizSession.class);
            QuizAnswer answer = QuizAnswer.builder()
                    .session(session)
                    .question(question)
                    .selectedOptions(List.of())
                    .build();

            // When
            QuizAnswerDto dto = QuizAnswerDto.from(answer);

            // Then
            assertThat(dto.questionId()).isEqualTo(3L);
            assertThat(dto.selectedOptions()).isEmpty();
        }
    }
}