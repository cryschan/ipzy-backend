package com.ipzy.domain.recommendation.dto.response;

import com.ipzy._global.common.enums.ClothingCategory;
import com.ipzy.domain.recommendation.entity.RecommendationItem;

import java.util.Objects;

/**
 * 추천 아이템 DTO - Python FastAPI 응답용
 */
public record RecommendedItemDto(
        Long productId,
        String category,
        String name,
        String brand,
        Integer price,
        String imageUrl,
        String linkUrl
) {
    private static final String DEFAULT_PRODUCT_NAME = "상품명 없음";
    private static final int DEFAULT_PRICE = 0;

    /**
     * Entity로 변환
     *
     * @param displayOrder 표시 순서
     * @param category     파싱된 카테고리
     * @return RecommendationItem 엔티티
     */
    public RecommendationItem toEntity(int displayOrder, ClothingCategory category) {
        return RecommendationItem.builder()
                .productId(this.productId)
                .category(category)
                .displayOrder(displayOrder)
                .productNameSnapshot(Objects.requireNonNullElse(this.name, DEFAULT_PRODUCT_NAME))
                .brandSnapshot(this.brand)
                .priceSnapshot(Objects.requireNonNullElse(this.price, DEFAULT_PRICE))
                .imageUrlSnapshot(this.imageUrl)
                .linkUrlSnapshot(this.linkUrl)
                .build();
    }
}
