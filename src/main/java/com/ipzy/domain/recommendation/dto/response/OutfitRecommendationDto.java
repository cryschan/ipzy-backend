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
        @JsonProperty("total_price")
        Integer totalPrice,
        @JsonProperty("style_board_url")
        String styleBoardUrl,
        List<RecommendedItemDto> items
) {
    /**
     * DTO를 Recommendation 엔티티로 변환
     */
    public Recommendation toEntity(QuizSession session, User user) {
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
                .build();
    }
}