package com.ipzy.domain.recommendation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ipzy.domain.quiz.entity.QuizSession;
import com.ipzy.domain.recommendation.entity.Recommendation;
import com.ipzy.domain.user.entity.User;

import java.util.List;

/**
 * 개별 코디 추천 DTO - Python FastAPI 응답용
 */
public record OutfitRecommendationDto(
        Integer displayOrder,
        String occasion,
        String season,
        String style,
        String reason,
        String status,
        @JsonProperty("job_id")
        String jobId,
        @JsonProperty("created_at")
        String createdAt,
        @JsonProperty("completed_at")
        String completedAt,
        OutfitResultDto result,
        String error
) {
    /**
     * DTO를 Recommendation 엔티티로 변환
     */
    public Recommendation toEntity(QuizSession session, User user) {
        Integer totalPrice = result != null ? result.totalPrice() : 0;
        String styleBoardUrl = result != null ? result.compositeImageUrl() : null;
        Integer imageWidth = result != null ? result.imageWidth() : null;
        Integer imageHeight = result != null ? result.imageHeight() : null;

        return Recommendation.builder()
                .session(session)
                .user(user)
                .displayOrder(displayOrder)
                .occasion(occasion)
                .season(season)
                .style(style)
                .reason(reason)
                .totalPrice(totalPrice != null ? totalPrice : 0)
                .styleBoardUrl(styleBoardUrl)
                .imageWidth(imageWidth)
                .imageHeight(imageHeight)
                .build();
    }

    /**
     * result.items 반환 (null-safe)
     */
    public List<RecommendedItemDto> items() {
        return result != null ? result.items() : List.of();
    }
}