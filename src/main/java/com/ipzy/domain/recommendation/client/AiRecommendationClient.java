package com.ipzy.domain.recommendation.client;

import com.ipzy.domain.recommendation.dto.request.RecommendationRequest;
import com.ipzy.domain.recommendation.dto.response.RecommendationResponse;

/**
 * AI 추천 서비스 클라이언트 인터페이스
 * <p>
 * 구현체:
 * - {@link PythonAiClient}: 실제 Python FastAPI 통신 (production)
 * - {@link MockAiClient}: Mock 응답 (local/dev)
 */
public interface AiRecommendationClient {

    /**
     * AI 서비스에 코디 추천 요청
     *
     * @param request 추천 요청 (sessionId, answers)
     * @return 추천 응답 (recommendedOutfits)
     */
    RecommendationResponse requestRecommendation(RecommendationRequest request);

    /**
     * AI 서비스 연결 테스트
     *
     * @param msg 테스트 메시지
     * @return 응답 메시지
     */
    String testConnection(String msg);
}
