package com.ipzy.domain.recommendation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Python FastAPI 추천 응답 DTO (최상위)
 */
public record RecommendationResponse(
        @JsonProperty("recommended_outfits")
        List<OutfitRecommendationDto> recommendedOutfits
) {
}
