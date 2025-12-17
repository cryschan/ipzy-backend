package com.ipzy.domain.recommendation.dto.response;

import com.ipzy.domain.quiz.entity.QuizSession;
import com.ipzy.domain.recommendation.entity.Recommendation;
import com.ipzy.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("OutfitRecommendationDto 테스트")
class OutfitRecommendationDtoTest {

    @Nested
    @DisplayName("toEntity 메서드")
    class ToEntity {

        @Test
        @DisplayName("성공 - DTO를 Recommendation 엔티티로 변환한다")
        void success() {
            // Given
            List<RecommendedItemDto> items = List.of(
                    new RecommendedItemDto(101L, "TOP", "오버핏 셔츠", "무신사 스탠다드", 59000,
                            "https://example.com/img1.jpg", "https://example.com/product1",
                            new ItemPositionDto(60, 100, 480, 576)),
                    new RecommendedItemDto(102L, "BOTTOM", "와이드 팬츠", "커버낫", 79000,
                            "https://example.com/img2.jpg", "https://example.com/product2",
                            new ItemPositionDto(0, 800, 600, 720)),
                    new RecommendedItemDto(103L, "SHOES", "화이트 스니커즈", "나이키", 99000,
                            "https://example.com/img3.jpg", "https://example.com/product3",
                            new ItemPositionDto(704, 1100, 392, 392))
            );

            OutfitResultDto result = new OutfitResultDto(
                    true, "Composite image created successfully",
                    "https://example.com/style_board1.jpg", 1200, 1600, 237000, items
            );

            OutfitRecommendationDto dto = new OutfitRecommendationDto(
                    1, "데이트", "봄", "캐주얼",
                    "밝은 색감의 캐주얼 룩으로 데이트에 적합합니다.",
                    "completed", "job-123", "2025-12-15T08:32:14Z", "2025-12-15T08:32:17Z",
                    result, null
            );

            QuizSession session = mock(QuizSession.class);
            User user = mock(User.class);

            // When
            Recommendation recommendation = dto.toEntity(session, user);

            // Then
            assertThat(recommendation.getSession()).isEqualTo(session);
            assertThat(recommendation.getUser()).isEqualTo(user);
            assertThat(recommendation.getDisplayOrder()).isEqualTo(1);
            assertThat(recommendation.getOccasion()).isEqualTo("데이트");
            assertThat(recommendation.getSeason()).isEqualTo("봄");
            assertThat(recommendation.getStyle()).isEqualTo("캐주얼");
            assertThat(recommendation.getReason()).isEqualTo("밝은 색감의 캐주얼 룩으로 데이트에 적합합니다.");
            assertThat(recommendation.getTotalPrice()).isEqualTo(237000);
            assertThat(recommendation.getStyleBoardUrl()).isEqualTo("https://example.com/style_board1.jpg");
        }

        @Test
        @DisplayName("성공 - 익명 사용자(user=null)도 정상 처리")
        void success_anonymousUser() {
            // Given
            OutfitResultDto result = new OutfitResultDto(
                    true, "Success",
                    "https://example.com/style_board2.jpg", 1200, 1600, 180000, List.of()
            );

            OutfitRecommendationDto dto = new OutfitRecommendationDto(
                    2, "출근", "가을", "포멀",
                    "깔끔한 포멀 룩입니다.",
                    "completed", "job-456", "2025-12-15T08:32:14Z", "2025-12-15T08:32:17Z",
                    result, null
            );

            QuizSession session = mock(QuizSession.class);

            // When
            Recommendation recommendation = dto.toEntity(session, null);

            // Then
            assertThat(recommendation.getUser()).isNull();
            assertThat(recommendation.getSession()).isEqualTo(session);
            assertThat(recommendation.isAnonymous()).isTrue();
        }

        @Test
        @DisplayName("성공 - 아이템이 비어있어도 정상 처리")
        void success_emptyItems() {
            // Given
            OutfitResultDto result = new OutfitResultDto(
                    true, "Success", null, 1200, 1600, 0, List.of()
            );

            OutfitRecommendationDto dto = new OutfitRecommendationDto(
                    1, "일상", "여름", "미니멀",
                    "심플한 미니멀 룩입니다.",
                    "completed", "job-789", "2025-12-15T08:32:14Z", "2025-12-15T08:32:17Z",
                    result, null
            );

            QuizSession session = mock(QuizSession.class);
            User user = mock(User.class);

            // When
            Recommendation recommendation = dto.toEntity(session, user);

            // Then
            assertThat(recommendation.getItems()).isEmpty();
        }

        @Test
        @DisplayName("성공 - result가 null이면 totalPrice는 0, styleBoardUrl은 null로 처리")
        void success_nullResult() {
            // Given
            OutfitRecommendationDto dto = new OutfitRecommendationDto(
                    1, "일상", "여름", "캐주얼",
                    "캐주얼 룩입니다.",
                    "failed", "job-000", "2025-12-15T08:32:14Z", null,
                    null, "AI service error"
            );

            QuizSession session = mock(QuizSession.class);
            User user = mock(User.class);

            // When
            Recommendation recommendation = dto.toEntity(session, user);

            // Then
            assertThat(recommendation.getTotalPrice()).isEqualTo(0);
            assertThat(recommendation.getStyleBoardUrl()).isNull();
        }

        @Test
        @DisplayName("성공 - items() 메서드는 result.items를 반환한다")
        void success_itemsMethod() {
            // Given
            List<RecommendedItemDto> items = List.of(
                    new RecommendedItemDto(101L, "TOP", "셔츠", "브랜드", 50000,
                            null, "https://example.com/product1", null)
            );

            OutfitResultDto result = new OutfitResultDto(
                    true, "Success", "https://example.com/board.jpg", 1200, 1600, 50000, items
            );

            OutfitRecommendationDto dto = new OutfitRecommendationDto(
                    1, "일상", "봄", "캐주얼", "룩입니다.",
                    "completed", "job-123", "2025-12-15T08:32:14Z", "2025-12-15T08:32:17Z",
                    result, null
            );

            // When & Then
            assertThat(dto.items()).hasSize(1);
            assertThat(dto.items().get(0).productId()).isEqualTo(101L);
        }

        @Test
        @DisplayName("성공 - result가 null이면 items()는 빈 리스트 반환")
        void success_itemsMethodWithNullResult() {
            // Given
            OutfitRecommendationDto dto = new OutfitRecommendationDto(
                    1, "일상", "봄", "캐주얼", "룩입니다.",
                    "failed", "job-123", "2025-12-15T08:32:14Z", null,
                    null, "error"
            );

            // When & Then
            assertThat(dto.items()).isEmpty();
        }
    }
}
