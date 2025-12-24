package com.ipzy.domain.recommendation.dto;

import com.ipzy.domain.recommendation.dto.response.RecommendationSummaryResponse;
import com.ipzy.domain.recommendation.entity.Recommendation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 추천 생성 상태 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "추천 생성 상태 응답")
public class RecommendationStatusResponse {

    @Schema(description = "퀴즈 세션 ID", example = "12")
    private Long sessionId;

    @Schema(
            description = "추천 생성 상태",
            example = "completed",
            allowableValues = {"not_started", "generating", "completed", "failed"}
    )
    private String status;

    @Schema(description = "생성된 추천 목록 (status가 'completed'일 때만 포함)", example = "[]")
    private List<RecommendationSummaryResponse> recommendations;

    /**
     * 추천 생성 상태를 반환합니다.
     *
     * @param sessionId 세션 ID
     * @param recommendations 추천 목록 (없으면 null)
     * @return 상태 응답
     */
    public static RecommendationStatusResponse of(Long sessionId, List<Recommendation> recommendations) {
        String status;
        List<RecommendationSummaryResponse> recommendationList = null;

        if (recommendations == null || recommendations.isEmpty()) {
            status = "not_started";
        } else {
            status = "completed";
            recommendationList = recommendations.stream()
                    .map(RecommendationSummaryResponse::from)
                    .toList();
        }

        return RecommendationStatusResponse.builder()
                .sessionId(sessionId)
                .status(status)
                .recommendations(recommendationList)
                .build();
    }
}

