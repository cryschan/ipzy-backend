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

@DisplayName("RecommendationRequest 테스트")
class RecommendationRequestTest {

    @Nested
    @DisplayName("of 메서드")
    class Of {

        @Test
        @DisplayName("성공 - QuizSession으로 RecommendationRequest 생성")
        void success() {
            // Given
            QuizSession session = mock(QuizSession.class);
            when(session.getId()).thenReturn(100L);

            QuizQuestion question1 = mock(QuizQuestion.class);
            when(question1.getId()).thenReturn(1L);
            when(question1.getText()).thenReturn("선호하는 스타일은?");

            QuizQuestion question2 = mock(QuizQuestion.class);
            when(question2.getId()).thenReturn(2L);
            when(question2.getText()).thenReturn("선호하는 색상은?");

            QuizAnswer answer1 = QuizAnswer.builder()
                    .session(session)
                    .question(question1)
                    .selectedOptions(List.of("캐주얼", "스트릿"))
                    .build();

            QuizAnswer answer2 = QuizAnswer.builder()
                    .session(session)
                    .question(question2)
                    .selectedOptions(List.of("블랙"))
                    .build();

            when(session.getAnswers()).thenReturn(List.of(answer1, answer2));

            // When
            RecommendationRequest request = RecommendationRequest.of(session);

            // Then
            assertThat(request.sessionId()).isEqualTo(100L);
            assertThat(request.answers()).hasSize(2);
            assertThat(request.answers().get(0).questionId()).isEqualTo(1L);
            assertThat(request.answers().get(0).selectedOptions()).containsExactly("캐주얼", "스트릿");
            assertThat(request.answers().get(1).questionId()).isEqualTo(2L);
            assertThat(request.answers().get(1).selectedOptions()).containsExactly("블랙");
        }

        @Test
        @DisplayName("성공 - 답변이 없는 세션도 정상 처리")
        void success_emptyAnswers() {
            // Given
            QuizSession session = mock(QuizSession.class);
            when(session.getId()).thenReturn(200L);
            when(session.getAnswers()).thenReturn(List.of());

            // When
            RecommendationRequest request = RecommendationRequest.of(session);

            // Then
            assertThat(request.sessionId()).isEqualTo(200L);
            assertThat(request.answers()).isEmpty();
        }
    }
}
