package com.ipzy.domain.recommendation.dto.response;

import com.ipzy.domain.recommendation.entity.RecommendationItem;

/**
 * 추천 아이템 응답 DTO - 클라이언트용
 */
public record RecommendationItemResponse(
        Long productId,
        String category,
        String name,
        String brand,
        Integer price,
        String imageUrl,
        String linkUrl,
        PositionResponse position
) {
    public static RecommendationItemResponse from(RecommendationItem item) {
        PositionResponse position = null;
        if (item.getPositionX() != null) {
            position = new PositionResponse(
                    item.getPositionX(),
                    item.getPositionY(),
                    item.getPositionWidth(),
                    item.getPositionHeight()
            );
        }

        return new RecommendationItemResponse(
                item.getProductId(),
                item.getCategory().name(),
                item.getProductNameSnapshot(),
                item.getBrandSnapshot(),
                item.getPriceSnapshot(),
                item.getImageUrlSnapshot(),
                item.getLinkUrlSnapshot(),
                position
        );
    }

    public record PositionResponse(
            Integer x,
            Integer y,
            Integer width,
            Integer height
    ) {}
}
