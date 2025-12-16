package com.ipzy.domain.recommendation.entity;

import com.ipzy._global.common.enums.ClothingCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecommendationItem 엔티티 테스트")
class RecommendationItemTest {

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("모든 필드로 RecommendationItem 생성")
        void createWithAllFields() {
            // When
            RecommendationItem item = RecommendationItem.builder()
                    .productId(1L)
                    .category(ClothingCategory.TOP)
                    .displayOrder(1)
                    .productNameSnapshot("오버핏 셔츠")
                    .brandSnapshot("무신사 스탠다드")
                    .priceSnapshot(59000)
                    .imageUrlSnapshot("https://example.com/image.jpg")
                    .linkUrlSnapshot("https://example.com/product/1")
                    .build();

            // Then
            assertThat(item.getProductId()).isEqualTo(1L);
            assertThat(item.getCategory()).isEqualTo(ClothingCategory.TOP);
            assertThat(item.getDisplayOrder()).isEqualTo(1);
            assertThat(item.getProductNameSnapshot()).isEqualTo("오버핏 셔츠");
            assertThat(item.getBrandSnapshot()).isEqualTo("무신사 스탠다드");
            assertThat(item.getPriceSnapshot()).isEqualTo(59000);
            assertThat(item.getImageUrlSnapshot()).isEqualTo("https://example.com/image.jpg");
            assertThat(item.getLinkUrlSnapshot()).isEqualTo("https://example.com/product/1");
        }

        @Test
        @DisplayName("필수 필드만으로 생성")
        void createWithRequiredFieldsOnly() {
            // When
            RecommendationItem item = RecommendationItem.builder()
                    .productId(1L)
                    .category(ClothingCategory.SHOES)
                    .productNameSnapshot("스니커즈")
                    .priceSnapshot(99000)
                    .build();

            // Then
            assertThat(item.getProductId()).isEqualTo(1L);
            assertThat(item.getCategory()).isEqualTo(ClothingCategory.SHOES);
            assertThat(item.getProductNameSnapshot()).isEqualTo("스니커즈");
            assertThat(item.getPriceSnapshot()).isEqualTo(99000);
        }

        @Test
        @DisplayName("기본값 적용 확인")
        void createWithDefaults() {
            // When
            RecommendationItem item = RecommendationItem.builder()
                    .productId(1L)
                    .category(ClothingCategory.TOP)
                    .productNameSnapshot("테스트")
                    .build();

            // Then
            assertThat(item.getDisplayOrder()).isEqualTo(0);
            assertThat(item.getPriceSnapshot()).isEqualTo(0);
            assertThat(item.getBrandSnapshot()).isNull();
            assertThat(item.getImageUrlSnapshot()).isNull();
            assertThat(item.getLinkUrlSnapshot()).isNull();
        }
    }

    @Nested
    @DisplayName("카테고리별 생성")
    class CategoryCreation {

        @Test
        @DisplayName("TOP 카테고리 아이템 생성")
        void createTopItem() {
            // When
            RecommendationItem item = RecommendationItem.builder()
                    .productId(1L)
                    .category(ClothingCategory.TOP)
                    .productNameSnapshot("셔츠")
                    .priceSnapshot(50000)
                    .build();

            // Then
            assertThat(item.getCategory()).isEqualTo(ClothingCategory.TOP);
        }

        @Test
        @DisplayName("BOTTOM 카테고리 아이템 생성")
        void createBottomItem() {
            // When
            RecommendationItem item = RecommendationItem.builder()
                    .productId(2L)
                    .category(ClothingCategory.BOTTOM)
                    .productNameSnapshot("팬츠")
                    .priceSnapshot(70000)
                    .build();

            // Then
            assertThat(item.getCategory()).isEqualTo(ClothingCategory.BOTTOM);
        }

        @Test
        @DisplayName("SHOES 카테고리 아이템 생성")
        void createShoesItem() {
            // When
            RecommendationItem item = RecommendationItem.builder()
                    .productId(3L)
                    .category(ClothingCategory.SHOES)
                    .productNameSnapshot("스니커즈")
                    .priceSnapshot(120000)
                    .build();

            // Then
            assertThat(item.getCategory()).isEqualTo(ClothingCategory.SHOES);
        }

        @Test
        @DisplayName("UNKNOWN 카테고리 아이템 생성")
        void createUnknownCategoryItem() {
            // When
            RecommendationItem item = RecommendationItem.builder()
                    .productId(4L)
                    .category(ClothingCategory.UNKNOWN)
                    .productNameSnapshot("기타 아이템")
                    .priceSnapshot(30000)
                    .build();

            // Then
            assertThat(item.getCategory()).isEqualTo(ClothingCategory.UNKNOWN);
        }
    }

    @Nested
    @DisplayName("스냅샷 데이터")
    class SnapshotData {

        @Test
        @DisplayName("상품 스냅샷 정보 저장")
        void storesProductSnapshot() {
            // Given
            String productName = "프리미엄 울 코트";
            String brand = "무신사 스탠다드";
            Integer price = 189000;
            String imageUrl = "https://cdn.example.com/coat.jpg";
            String linkUrl = "https://www.example.com/products/coat";

            // When
            RecommendationItem item = RecommendationItem.builder()
                    .productId(100L)
                    .category(ClothingCategory.TOP)
                    .productNameSnapshot(productName)
                    .brandSnapshot(brand)
                    .priceSnapshot(price)
                    .imageUrlSnapshot(imageUrl)
                    .linkUrlSnapshot(linkUrl)
                    .build();

            // Then
            assertThat(item.getProductNameSnapshot()).isEqualTo(productName);
            assertThat(item.getBrandSnapshot()).isEqualTo(brand);
            assertThat(item.getPriceSnapshot()).isEqualTo(price);
            assertThat(item.getImageUrlSnapshot()).isEqualTo(imageUrl);
            assertThat(item.getLinkUrlSnapshot()).isEqualTo(linkUrl);
        }

        @Test
        @DisplayName("nullable 필드 null 허용")
        void allowsNullableFields() {
            // When
            RecommendationItem item = RecommendationItem.builder()
                    .productId(1L)
                    .category(ClothingCategory.TOP)
                    .productNameSnapshot("테스트 상품")
                    .priceSnapshot(10000)
                    .brandSnapshot(null)
                    .imageUrlSnapshot(null)
                    .linkUrlSnapshot(null)
                    .build();

            // Then
            assertThat(item.getBrandSnapshot()).isNull();
            assertThat(item.getImageUrlSnapshot()).isNull();
            assertThat(item.getLinkUrlSnapshot()).isNull();
        }
    }
}
