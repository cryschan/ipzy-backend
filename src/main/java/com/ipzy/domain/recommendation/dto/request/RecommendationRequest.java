package com.ipzy.domain.recommendation.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ipzy.domain.quiz.entity.QuizSession;

import java.util.List;
import java.util.Set;

/**
 * Python FastAPI 추천 요청 DTO
 * - 퀴즈 답변만 전송 (상품 정보는 Python에서 직접 DB 조회)
 * - excludeCombinations: 중복 회피를 위한 기존 조합 목록
 */
public record RecommendationRequest(
        Long sessionId,
        List<QuizAnswerDto> answers,
        @JsonProperty("exclude_combinations")
        List<Set<Long>> excludeCombinations
) {
    public static RecommendationRequest of(QuizSession session) {
        return new RecommendationRequest(
                session.getId(),
                session.getAnswers().stream()
                        .map(QuizAnswerDto::from)
                        .toList(),
                List.of()  // 기본값: 빈 리스트
        );
    }

    /**
     * 기존 추천 조합을 제외하고 생성
     */
    public static RecommendationRequest of(QuizSession session, List<Set<Long>> excludeCombinations) {
        return new RecommendationRequest(
                session.getId(),
                session.getAnswers().stream()
                        .map(QuizAnswerDto::from)
                        .toList(),
                excludeCombinations
        );
    }
}
