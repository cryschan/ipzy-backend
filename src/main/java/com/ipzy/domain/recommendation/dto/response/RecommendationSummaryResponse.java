package com.ipzy.domain.recommendation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ipzy.domain.recommendation.entity.Recommendation;

import java.util.List;

/**
 * 추천 요약 응답 DTO - 클라이언트용 (Python 응답 형식)
 */
public record RecommendationSummaryResponse(
        Integer displayOrder,
        String occasion,
        String season,
        String style,
        String reason,
        String status,
        @JsonProperty("jobId")
        String jobId,
        @JsonProperty("createdAt")
        String createdAt,
        @JsonProperty("completedAt")
        String completedAt,
        ResultResponse result,
        String error
) {
    public static RecommendationSummaryResponse from(Recommendation recommendation) {
        ResultResponse result = new ResultResponse(
                true,
                "Recommendation loaded successfully",
                recommendation.getStyleBoardUrl(),
                recommendation.getImageWidth(),
                recommendation.getImageHeight(),
                recommendation.getTotalPrice(),
                recommendation.getItems().stream()
                        .map(RecommendationItemResponse::from)
                        .toList()
        );

        String createdAt = recommendation.getCreatedAt() != null
                ? recommendation.getCreatedAt().toString()
                : null;

        return new RecommendationSummaryResponse(
                recommendation.getDisplayOrder(),
                recommendation.getOccasion(),
                recommendation.getSeason(),
                recommendation.getStyle(),
                recommendation.getReason(),
                "completed",
                "rec-" + recommendation.getId(),
                createdAt,
                createdAt,
                result,
                null
        );
    }

    public record ResultResponse(
            Boolean success,
            String message,
            @JsonProperty("compositeImageUrl")
            String compositeImageUrl,
            @JsonProperty("imageWidth")
            Integer imageWidth,
            @JsonProperty("imageHeight")
            Integer imageHeight,
            @JsonProperty("totalPrice")
            Integer totalPrice,
            List<RecommendationItemResponse> items
    ) {}
}
