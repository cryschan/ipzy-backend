package com.ipzy.domain.recommendation.dto.response;

import java.util.List;

/**
 * 코디 추천 결과 DTO - Python FastAPI 응답용
 * OutfitRecommendationDto의 result 필드에 매핑
 */
public record OutfitResultDto(
        Boolean success,
        String message,
        String compositeImageUrl,
        Integer imageWidth,
        Integer imageHeight,
        Integer totalPrice,
        List<RecommendedItemDto> items
) {
}
