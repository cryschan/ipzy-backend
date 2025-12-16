package com.ipzy.domain.recommendation.service;

import com.ipzy.domain.quiz.entity.Quiz;
import com.ipzy.domain.quiz.entity.QuizSession;
import com.ipzy.domain.quiz.repository.QuizSessionRepository;
import com.ipzy.domain.recommendation.client.AiRecommendationClient;
import com.ipzy.domain.recommendation.dto.response.OutfitRecommendationDto;
import com.ipzy.domain.recommendation.dto.response.RecommendationResponse;
import com.ipzy.domain.recommendation.dto.response.RecommendedItemDto;
import com.ipzy.domain.recommendation.entity.Recommendation;
import com.ipzy.domain.recommendation.exception.RecommendationException;
import com.ipzy.domain.recommendation.repository.RecommendationRepository;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RecommendationService 테스트")
class RecommendationServiceTest {

    @Mock
    private QuizSessionRepository quizSessionRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private UserService userService;

    @Mock
    private AiRecommendationClient aiClient;

    @InjectMocks
    private RecommendationService recommendationService;

    private QuizSession completedSession;
    private User user;
    private Quiz quiz;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        quiz = mock(Quiz.class);

        completedSession = QuizSession.builder()
                .user(user)
                .quiz(quiz)
                .build();
        completedSession.complete();
        ReflectionTestUtils.setField(completedSession, "id", 100L);
    }

    @Nested
    @DisplayName("generateRecommendation")
    class GenerateRecommendation {

        @Test
        @DisplayName("성공 - 추천 생성 및 저장")
        void success() {
            // Given
            Long sessionId = 100L;

            RecommendationResponse aiResponse = new RecommendationResponse(
                    List.of(
                            new OutfitRecommendationDto(
                                    1, "데이트", "봄", "캐주얼",
                                    "밝은 색감의 캐주얼 룩입니다.",
                                    237000,
                                    "https://example.com/style_board1.jpg",
                                    List.of(
                                            new RecommendedItemDto(1L, "TOP", "오버핏 셔츠", "무신사 스탠다드", 59000, "https://example.com/img1.jpg", "https://example.com/product1"),
                                            new RecommendedItemDto(2L, "BOTTOM", "와이드 팬츠", "커버낫", 79000, "https://example.com/img2.jpg", "https://example.com/product2"),
                                            new RecommendedItemDto(3L, "SHOES", "화이트 스니커즈", "나이키", 99000, "https://example.com/img3.jpg", "https://example.com/product3")
                                    )
                            )
                    )
            );

            Long currentUserId = 1L; // 세션 소유자와 동일

            given(quizSessionRepository.findByIdWithAnswers(sessionId))
                    .willReturn(Optional.of(completedSession));
            given(userService.findActiveUser(currentUserId))
                    .willReturn(user);
            given(recommendationRepository.existsBySessionId(sessionId))
                    .willReturn(false);
            given(aiClient.requestRecommendation(any()))
                    .willReturn(aiResponse);
            given(recommendationRepository.saveAll(anyList()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Recommendation> result = recommendationService.generateRecommendation(sessionId, currentUserId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOccasion()).isEqualTo("데이트");
            assertThat(result.get(0).getStyle()).isEqualTo("캐주얼");
            verify(recommendationRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("실패 - 세션 없음")
        void fail_sessionNotFound() {
            // Given
            Long sessionId = 999L;
            Long currentUserId = 1L;
            given(quizSessionRepository.findByIdWithAnswers(sessionId))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> recommendationService.generateRecommendation(sessionId, currentUserId))
                    .isInstanceOf(RecommendationException.class)
                    .hasMessageContaining("세션");
        }

        @Test
        @DisplayName("실패 - 세션 미완료")
        void fail_sessionNotCompleted() {
            // Given
            Long sessionId = 100L;
            Long currentUserId = 1L;
            QuizSession incompleteSession = QuizSession.builder()
                    .user(user)
                    .quiz(quiz)
                    .build();
            // complete() 호출 안 함
            ReflectionTestUtils.setField(incompleteSession, "id", sessionId);

            given(quizSessionRepository.findByIdWithAnswers(sessionId))
                    .willReturn(Optional.of(incompleteSession));

            // When & Then
            assertThatThrownBy(() -> recommendationService.generateRecommendation(sessionId, currentUserId))
                    .isInstanceOf(RecommendationException.class)
                    .hasMessageContaining("세션");
        }

        @Test
        @DisplayName("실패 - 이미 추천 존재")
        void fail_recommendationAlreadyExists() {
            // Given
            Long sessionId = 100L;
            Long currentUserId = 1L;
            given(quizSessionRepository.findByIdWithAnswers(sessionId))
                    .willReturn(Optional.of(completedSession));
            given(userService.findActiveUser(currentUserId))
                    .willReturn(user);
            given(recommendationRepository.existsBySessionId(sessionId))
                    .willReturn(true);

            // When & Then
            assertThatThrownBy(() -> recommendationService.generateRecommendation(sessionId, currentUserId))
                    .isInstanceOf(RecommendationException.class)
                    .hasMessageContaining("세션");
        }
    }

    @Nested
    @DisplayName("getRecommendationsBySession")
    class GetRecommendationsBySession {

        @Test
        @DisplayName("성공 - 세션별 추천 조회")
        void success() {
            // Given
            Long sessionId = 100L;
            Long currentUserId = 1L;
            Recommendation recommendation = Recommendation.builder()
                    .session(completedSession)
                    .user(user)
                    .displayOrder(1)
                    .occasion("데이트")
                    .build();
            ReflectionTestUtils.setField(recommendation, "id", 1L);

            given(quizSessionRepository.findById(sessionId))
                    .willReturn(Optional.of(completedSession));
            given(recommendationRepository.findBySessionIdWithItems(sessionId))
                    .willReturn(List.of(recommendation));

            // When
            List<Recommendation> result = recommendationService.getRecommendationsBySession(sessionId, currentUserId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOccasion()).isEqualTo("데이트");
        }
    }
}
