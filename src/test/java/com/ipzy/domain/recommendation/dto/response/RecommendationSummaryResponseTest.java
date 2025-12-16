package com.ipzy.domain.recommendation.dto.response;

import com.ipzy._global.common.enums.ClothingCategory;
import com.ipzy.domain.recommendation.entity.Recommendation;
import com.ipzy.domain.recommendation.entity.RecommendationItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecommendationSummaryResponse 테스트")
class RecommendationSummaryResponseTest {

    @Nested
    @DisplayName("from 메서드")
    class From {

        @Test
        @DisplayName("성공 - Recommendation 엔티티를 응답 DTO로 변환한다")
        void success() {
            // Given
            Recommendation recommendation = Recommendation.builder()
                    .displayOrder(1)
                    .occasion("데이트")
                    .season("봄")
                    .style("캐주얼")
                    .reason("밝은 색감의 캐주얼 룩입니다.")
                    .totalPrice(130000)
                    .styleBoardUrl("https://example.com/style_board1.jpg")
                    .imageWidth(1200)
                    .imageHeight(1600)
                    .build();
            ReflectionTestUtils.setField(recommendation, "id", 1L);

            RecommendationItem item1 = RecommendationItem.builder()
                    .productId(101L)
                    .category(ClothingCategory.TOP)
                    .displayOrder(1)
                    .productNameSnapshot("화이트 티셔츠")
                    .brandSnapshot("무신사 스탠다드")
                    .priceSnapshot(50000)
                    .imageUrlSnapshot("https://example.com/top.jpg")
                    .linkUrlSnapshot("https://example.com/product1")
                    .positionX(60)
                    .positionY(100)
                    .positionWidth(480)
                    .positionHeight(576)
                    .build();

            RecommendationItem item2 = RecommendationItem.builder()
                    .productId(102L)
                    .category(ClothingCategory.BOTTOM)
                    .displayOrder(2)
                    .productNameSnapshot("데님 팬츠")
                    .brandSnapshot("커버낫")
                    .priceSnapshot(80000)
                    .imageUrlSnapshot("https://example.com/bottom.jpg")
                    .linkUrlSnapshot("https://example.com/product2")
                    .positionX(0)
                    .positionY(800)
                    .positionWidth(600)
                    .positionHeight(720)
                    .build();

            recommendation.addItem(item1);
            recommendation.addItem(item2);

            // When
            RecommendationSummaryResponse response = RecommendationSummaryResponse.from(recommendation);

            // Then
            assertThat(response.displayOrder()).isEqualTo(1);
            assertThat(response.occasion()).isEqualTo("데이트");
            assertThat(response.season()).isEqualTo("봄");
            assertThat(response.style()).isEqualTo("캐주얼");
            assertThat(response.reason()).isEqualTo("밝은 색감의 캐주얼 룩입니다.");
            assertThat(response.status()).isEqualTo("completed");
            assertThat(response.jobId()).isEqualTo("rec-1");
            assertThat(response.error()).isNull();

            // result 검증
            assertThat(response.result()).isNotNull();
            assertThat(response.result().success()).isTrue();
            assertThat(response.result().totalPrice()).isEqualTo(130000);
            assertThat(response.result().compositeImageUrl()).isEqualTo("https://example.com/style_board1.jpg");
            assertThat(response.result().imageWidth()).isEqualTo(1200);
            assertThat(response.result().imageHeight()).isEqualTo(1600);
            assertThat(response.result().items()).hasSize(2);

            // item position 검증
            RecommendationItemResponse firstItem = response.result().items().get(0);
            assertThat(firstItem.position()).isNotNull();
            assertThat(firstItem.position().x()).isEqualTo(60);
            assertThat(firstItem.position().y()).isEqualTo(100);
        }

        @Test
        @DisplayName("성공 - 아이템이 없는 추천도 정상 변환")
        void success_emptyItems() {
            // Given
            Recommendation recommendation = Recommendation.builder()
                    .displayOrder(1)
                    .occasion("일상")
                    .season("여름")
                    .style("미니멀")
                    .reason("심플한 미니멀 룩입니다.")
                    .totalPrice(0)
                    .styleBoardUrl(null)
                    .build();
            ReflectionTestUtils.setField(recommendation, "id", 2L);

            // When
            RecommendationSummaryResponse response = RecommendationSummaryResponse.from(recommendation);

            // Then
            assertThat(response.jobId()).isEqualTo("rec-2");
            assertThat(response.result().items()).isEmpty();
            assertThat(response.result().totalPrice()).isEqualTo(0);
        }
    }
}
