package com.ipzy.domain.recommendation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 코디 추천 결과 DTO - Python FastAPI 응답용
 * OutfitRecommendationDto의 result 필드에 매핑
 */
public record OutfitResultDto(
        Boolean success,
        String message,
        @JsonProperty("composite_image_url")
        String compositeImageUrl,
        @JsonProperty("image_width")
        Integer imageWidth,
        @JsonProperty("image_height")
        Integer imageHeight,
        @JsonProperty("total_price")
        Integer totalPrice,
        List<RecommendedItemDto> items
) {
}
