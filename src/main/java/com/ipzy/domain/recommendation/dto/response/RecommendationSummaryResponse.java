package com.ipzy.domain.recommendation.dto.response;

import com.ipzy.domain.recommendation.entity.Recommendation;

import java.util.List;

/**
 * 추천 요약 응답 DTO - 클라이언트용
 */
public record RecommendationSummaryResponse(
        Long recommendationId,
        Integer displayOrder,
        String occasion,
        String season,
        String style,
        String reason,
        Integer totalPrice,
        String styleBoardUrl,
        List<RecommendationItemResponse> items
) {
    public static RecommendationSummaryResponse from(Recommendation recommendation) {
        return new RecommendationSummaryResponse(
                recommendation.getId(),
                recommendation.getDisplayOrder(),
                recommendation.getOccasion(),
                recommendation.getSeason(),
                recommendation.getStyle(),
                recommendation.getReason(),
                recommendation.getTotalPrice(),
                recommendation.getStyleBoardUrl(),
                recommendation.getItems().stream()
                        .map(RecommendationItemResponse::from)
                        .toList()
        );
    }
}
