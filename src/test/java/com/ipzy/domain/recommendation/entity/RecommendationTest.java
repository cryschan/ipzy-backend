package com.ipzy.domain.recommendation.entity;

import com.ipzy._global.common.enums.ClothingCategory;
import com.ipzy.domain.quiz.entity.QuizSession;
import com.ipzy.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Recommendation 엔티티 테스트")
class RecommendationTest {

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("모든 필드로 Recommendation 생성")
        void createWithAllFields() {
            // Given
            User user = mock(User.class);
            QuizSession session = mock(QuizSession.class);

            // When
            Recommendation recommendation = Recommendation.builder()
                    .user(user)
                    .session(session)
                    .displayOrder(1)
                    .totalPrice(150000)
                    .reason("테스트 추천 이유")
                    .occasion("데이트")
                    .season("봄")
                    .style("캐주얼")
                    .styleBoardUrl("https://example.com/style.jpg")
                    .build();

            // Then
            assertThat(recommendation.getUser()).isEqualTo(user);
            assertThat(recommendation.getSession()).isEqualTo(session);
            assertThat(recommendation.getDisplayOrder()).isEqualTo(1);
            assertThat(recommendation.getTotalPrice()).isEqualTo(150000);
            assertThat(recommendation.getReason()).isEqualTo("테스트 추천 이유");
            assertThat(recommendation.getOccasion()).isEqualTo("데이트");
            assertThat(recommendation.getSeason()).isEqualTo("봄");
            assertThat(recommendation.getStyle()).isEqualTo("캐주얼");
            assertThat(recommendation.getStyleBoardUrl()).isEqualTo("https://example.com/style.jpg");
        }

        @Test
        @DisplayName("익명 사용자로 Recommendation 생성 (user = null)")
        void createForAnonymousUser() {
            // When
            Recommendation recommendation = Recommendation.builder()
                    .user(null)
                    .displayOrder(1)
                    .build();

            // Then
            assertThat(recommendation.getUser()).isNull();
            assertThat(recommendation.isAnonymous()).isTrue();
        }

        @Test
        @DisplayName("기본값 적용 확인")
        void createWithDefaults() {
            // When
            Recommendation recommendation = Recommendation.builder().build();

            // Then
            assertThat(recommendation.getDisplayOrder()).isEqualTo(1);
            assertThat(recommendation.getTotalPrice()).isEqualTo(0);
            assertThat(recommendation.getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("아이템 관리")
    class ItemManagement {

        @Test
        @DisplayName("아이템 추가 시 양방향 관계 설정")
        void addItem_setsBidirectionalRelation() {
            // Given
            Recommendation recommendation = Recommendation.builder()
                    .displayOrder(1)
                    .build();

            RecommendationItem item = RecommendationItem.builder()
                    .productId(1L)
                    .category(ClothingCategory.TOP)
                    .priceSnapshot(50000)
                    .productNameSnapshot("테스트 상품")
                    .build();

            // When
            recommendation.addItem(item);

            // Then
            assertThat(recommendation.getItems()).hasSize(1);
            assertThat(recommendation.getItems().get(0)).isEqualTo(item);
            assertThat(item.getRecommendation()).isEqualTo(recommendation);
        }

        @Test
        @DisplayName("아이템 추가 시 총 가격 재계산")
        void addItem_recalculatesTotalPrice() {
            // Given
            Recommendation recommendation = Recommendation.builder()
                    .displayOrder(1)
                    .totalPrice(0)
                    .build();

            RecommendationItem item1 = RecommendationItem.builder()
                    .productId(1L)
                    .category(ClothingCategory.TOP)
                    .priceSnapshot(50000)
                    .productNameSnapshot("상의")
                    .build();

            RecommendationItem item2 = RecommendationItem.builder()
                    .productId(2L)
                    .category(ClothingCategory.BOTTOM)
                    .priceSnapshot(70000)
                    .productNameSnapshot("하의")
                    .build();

            // When
            recommendation.addItem(item1);
            recommendation.addItem(item2);

            // Then
            assertThat(recommendation.getTotalPrice()).isEqualTo(120000);
        }

        @Test
        @DisplayName("여러 아이템 추가")
        void addMultipleItems() {
            // Given
            Recommendation recommendation = Recommendation.builder()
                    .displayOrder(1)
                    .build();

            // When
            for (int i = 1; i <= 3; i++) {
                RecommendationItem item = RecommendationItem.builder()
                        .productId((long) i)
                        .category(ClothingCategory.TOP)
                        .priceSnapshot(10000 * i)
                        .productNameSnapshot("상품 " + i)
                        .build();
                recommendation.addItem(item);
            }

            // Then
            assertThat(recommendation.getItems()).hasSize(3);
            assertThat(recommendation.getTotalPrice()).isEqualTo(60000); // 10000 + 20000 + 30000
        }
    }

    @Nested
    @DisplayName("isAnonymous")
    class IsAnonymous {

        @Test
        @DisplayName("user가 null이면 true 반환")
        void returnsTrueWhenUserIsNull() {
            // Given
            Recommendation recommendation = Recommendation.builder()
                    .user(null)
                    .build();

            // Then
            assertThat(recommendation.isAnonymous()).isTrue();
        }

        @Test
        @DisplayName("user가 있으면 false 반환")
        void returnsFalseWhenUserExists() {
            // Given
            User user = mock(User.class);
            Recommendation recommendation = Recommendation.builder()
                    .user(user)
                    .build();

            // Then
            assertThat(recommendation.isAnonymous()).isFalse();
        }
    }
}
