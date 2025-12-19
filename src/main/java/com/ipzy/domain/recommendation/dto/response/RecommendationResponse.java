package com.ipzy.domain.recommendation.dto.response;

import java.util.List;

/**
 * Python FastAPI 추천 응답 DTO (최상위)
 */
public record RecommendationResponse(
        List<OutfitRecommendationDto> recommendedOutfits
) {
}
