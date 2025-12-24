package com.ipzy.domain.quiz.dto;

import com.ipzy.domain.recommendation.dto.response.RecommendationSummaryResponse;
import com.ipzy.domain.recommendation.entity.Recommendation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 퀴즈 세션 완료 + 추천 생성 응답 DTO
 * - autoGenerate=true일 때 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "퀴즈 세션 완료 및 추천 생성 응답")
public class QuizCompletionWithRecommendationsResponse {

    @Schema(description = "퀴즈 세션 ID", example = "12")
    private Long sessionId;

    @Schema(description = "세션 완료 여부", example = "true")
    private Boolean completed;

    @Schema(description = "세션이 완료된 시각", example = "2025-01-15T14:23:10")
    private LocalDateTime completedAt;

    @Schema(description = "생성된 추천 목록")
    private List<RecommendationSummaryResponse> recommendations;

    /**
     * QuizCompletionResponse와 추천 목록을 조합하여 생성
     *
     * @param completion      세션 완료 정보
     * @param recommendations 추천 목록 (null 또는 빈 리스트 가능)
     * @return 완료 + 추천 응답
     */
    public static QuizCompletionWithRecommendationsResponse of(
            QuizCompletionResponse completion,
            List<Recommendation> recommendations
    ) {
        List<RecommendationSummaryResponse> recommendationList = null;
        
        // null 또는 빈 리스트 체크 (RecommendationStatusResponse.of()와 일관성 유지)
        if (recommendations != null && !recommendations.isEmpty()) {
            recommendationList = recommendations.stream()
                    .map(RecommendationSummaryResponse::from)
                    .toList();
        }

        return QuizCompletionWithRecommendationsResponse.builder()
                .sessionId(completion.getSessionId())
                .completed(completion.getCompleted())
                .completedAt(completion.getCompletedAt())
                .recommendations(recommendationList)
                .build();
    }
}

