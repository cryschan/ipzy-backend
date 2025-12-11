package com.ipzy.domain.recommendation.dto.response;

import com.ipzy.domain.recommendation.entity.RecommendationItem;

/**
 * 추천 아이템 응답 DTO - 클라이언트용
 */
public record RecommendationItemResponse(
        Long itemId,
        Long productId,
        String category,
        Integer displayOrder,
        String productName,
        String brand,
        Integer price,
        String imageUrl,
        String linkUrl
) {
    public static RecommendationItemResponse from(RecommendationItem item) {
        return new RecommendationItemResponse(
                item.getId(),
                item.getProductId(),
                item.getCategory().name(),
                item.getDisplayOrder(),
                item.getProductNameSnapshot(),
                item.getBrandSnapshot(),
                item.getPriceSnapshot(),
                item.getImageUrlSnapshot(),
                item.getLinkUrlSnapshot()
        );
    }
}
