package com.ipzy.domain.recommendation.service;

import com.ipzy.domain.quiz.entity.Quiz;
import com.ipzy.domain.quiz.entity.QuizSession;
import com.ipzy.domain.quiz.repository.QuizSessionRepository;
import com.ipzy.domain.recommendation.client.AiRecommendationClient;
import com.ipzy.domain.recommendation.dto.response.OutfitRecommendationDto;
import com.ipzy.domain.recommendation.dto.response.OutfitResultDto;
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

            List<RecommendedItemDto> items = List.of(
                    new RecommendedItemDto(1L, "TOP", "오버핏 셔츠", "무신사 스탠다드", 59000, "https://example.com/img1.jpg", "https://example.com/product1", null),
                    new RecommendedItemDto(2L, "BOTTOM", "와이드 팬츠", "커버낫", 79000, "https://example.com/img2.jpg", "https://example.com/product2", null),
                    new RecommendedItemDto(3L, "SHOES", "화이트 스니커즈", "나이키", 99000, "https://example.com/img3.jpg", "https://example.com/product3", null)
            );

            OutfitResultDto outfitResult = new OutfitResultDto(
                    true, "Composite image created successfully",
                    "https://example.com/style_board1.jpg", 1200, 1600, 237000, items
            );

            RecommendationResponse aiResponse = new RecommendationResponse(
                    List.of(
                            new OutfitRecommendationDto(
                                    1, "데이트", "봄", "캐주얼",
                                    "밝은 색감의 캐주얼 룩입니다.",
                                    "completed", "job-123", "2025-12-15T08:32:14Z", "2025-12-15T08:32:17Z",
                                    outfitResult, null
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
            List<Recommendation> recommendations = recommendationService.generateRecommendation(sessionId, currentUserId);

            // Then
            assertThat(recommendations).hasSize(1);
            assertThat(recommendations.get(0).getOccasion()).isEqualTo("데이트");
            assertThat(recommendations.get(0).getStyle()).isEqualTo("캐주얼");
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
    @DisplayName("regenerateRecommendation")
    class RegenerateRecommendation {

        @Test
        @DisplayName("성공 - 이미 추천이 있어도 새로운 추천 생성")
        void success_evenIfRecommendationExists() {
            // Given
            Long sessionId = 100L;

            List<RecommendedItemDto> items = List.of(
                    new RecommendedItemDto(1L, "TOP", "오버핏 셔츠", "무신사 스탠다드", 59000, "https://example.com/img1.jpg", "https://example.com/product1", null)
            );

            OutfitResultDto outfitResult = new OutfitResultDto(
                    true, "Composite image created successfully",
                    "https://example.com/style_board2.jpg", 1200, 1600, 59000, items
            );

            RecommendationResponse aiResponse = new RecommendationResponse(
                    List.of(
                            new OutfitRecommendationDto(
                                    1, "출근", "봄", "미니멀",
                                    "깔끔한 오피스 룩입니다.",
                                    "completed", "job-456", "2025-12-15T08:32:14Z", "2025-12-15T08:32:17Z",
                                    outfitResult, null
                            )
                    )
            );

            Long currentUserId = 1L;

            given(quizSessionRepository.findByIdWithAnswers(sessionId))
                    .willReturn(Optional.of(completedSession));
            given(userService.findActiveUser(currentUserId))
                    .willReturn(user);
            // existsBySessionId 호출하지 않음 - regenerate는 중복 체크 없음
            given(aiClient.requestRecommendation(any()))
                    .willReturn(aiResponse);
            given(recommendationRepository.saveAll(anyList()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Recommendation> recommendations = recommendationService.regenerateRecommendation(sessionId, currentUserId);

            // Then
            assertThat(recommendations).hasSize(1);
            assertThat(recommendations.get(0).getOccasion()).isEqualTo("출근");
            assertThat(recommendations.get(0).getStyle()).isEqualTo("미니멀");
            verify(recommendationRepository).saveAll(anyList());
            // 중복 체크하지 않음 확인
            verify(recommendationRepository, never()).existsBySessionId(anyLong());
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
            assertThatThrownBy(() -> recommendationService.regenerateRecommendation(sessionId, currentUserId))
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
            ReflectionTestUtils.setField(incompleteSession, "id", sessionId);

            given(quizSessionRepository.findByIdWithAnswers(sessionId))
                    .willReturn(Optional.of(incompleteSession));

            // When & Then
            assertThatThrownBy(() -> recommendationService.regenerateRecommendation(sessionId, currentUserId))
                    .isInstanceOf(RecommendationException.class)
                    .hasMessageContaining("세션");
        }

        @Test
        @DisplayName("실패 - 다른 사용자 세션 접근")
        void fail_accessDenied() {
            // Given
            Long sessionId = 100L;
            Long currentUserId = 999L; // 다른 사용자

            User otherUser = mock(User.class);
            when(otherUser.getId()).thenReturn(currentUserId);

            given(quizSessionRepository.findByIdWithAnswers(sessionId))
                    .willReturn(Optional.of(completedSession));
            given(userService.findActiveUser(currentUserId))
                    .willReturn(otherUser);

            // When & Then
            assertThatThrownBy(() -> recommendationService.regenerateRecommendation(sessionId, currentUserId))
                    .isInstanceOf(RecommendationException.class)
                    .hasMessageContaining("권한");
        }
    }

    @Nested
    @DisplayName("getRecommendationsByUser")
    class GetRecommendationsByUser {

        @Test
        @DisplayName("성공 - 사용자별 추천 조회")
        void success() {
            // Given
            Long userId = 1L;
            Recommendation recommendation = Recommendation.builder()
                    .session(completedSession)
                    .user(user)
                    .displayOrder(1)
                    .occasion("데이트")
                    .build();
            ReflectionTestUtils.setField(recommendation, "id", 1L);

            given(recommendationRepository.findByUserIdWithItems(userId))
                    .willReturn(List.of(recommendation));

            // When
            List<Recommendation> result = recommendationService.getRecommendationsByUser(userId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOccasion()).isEqualTo("데이트");
        }

        @Test
        @DisplayName("성공 - 추천 없으면 빈 리스트 반환")
        void success_emptyList() {
            // Given
            Long userId = 999L;
            given(recommendationRepository.findByUserIdWithItems(userId))
                    .willReturn(List.of());

            // When
            List<Recommendation> result = recommendationService.getRecommendationsByUser(userId);

            // Then
            assertThat(result).isEmpty();
        }
    }

}
