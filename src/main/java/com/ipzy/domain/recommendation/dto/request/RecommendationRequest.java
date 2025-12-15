package com.ipzy.domain.recommendation.dto.request;

import com.ipzy.domain.quiz.entity.QuizSession;

import java.util.List;

/**
 * Python FastAPI 추천 요청 DTO
 * - 퀴즈 답변만 전송 (상품 정보는 Python에서 직접 DB 조회)
 */
public record RecommendationRequest(
        Long sessionId,
        List<QuizAnswerDto> answers
) {
    public static RecommendationRequest of(QuizSession session) {
        return new RecommendationRequest(
                session.getId(),
                session.getAnswers().stream()
                        .map(QuizAnswerDto::from)
                        .toList()
        );
    }
}
