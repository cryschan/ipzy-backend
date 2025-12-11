package com.ipzy.domain.recommendation.client;

import com.ipzy.domain.recommendation.dto.request.RecommendationRequest;
import com.ipzy.domain.recommendation.dto.response.RecommendationResponse;
import com.ipzy.domain.recommendation.exception.RecommendationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Python AI 서비스 통신 클라이언트 (Production)
 * <p>
 * 실제 Python FastAPI와 HTTP 통신을 수행합니다.
 * <p>
 * 활성화 조건: spring.profiles.active=prod
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class PythonAiClient implements AiRecommendationClient {

    private final RestClient pythonAiRestClient;

    @Override
    public RecommendationResponse requestRecommendation(RecommendationRequest request) {
        log.info("==== Python AI 추천 요청 시작 ====");
        log.info("세션 ID: {}, 답변 수: {}", request.sessionId(), request.answers().size());

        try {
            RecommendationResponse response = pythonAiRestClient.post()
                    .uri("/api/v1/recommend")
                    .body(request)
                    .retrieve()
                    .body(RecommendationResponse.class);

            if (response == null) {
                log.error("Python AI 서비스에서 null 응답 반환");
                throw RecommendationException.aiInvalidResponse();
            }

            log.info("Python AI 추천 응답 완료: {} 개 코디",
                    response.recommendedOutfits() != null ? response.recommendedOutfits().size() : 0);
            log.info("==== Python AI 추천 요청 완료 ====");

            return response;
        } catch (ResourceAccessException e) {
            log.error("Python AI 서비스 연결 실패: {}", e.getMessage());
            throw RecommendationException.aiServiceUnavailable();
        }
    }

    @Override
    public String testConnection(String msg) {
        log.info("==== Python 통신 테스트 시작 ====");
        log.info("보내는 메시지: {}", msg);

        try {
            String response = pythonAiRestClient.post()
                    .uri("/test")
                    .body(msg)
                    .retrieve()
                    .body(String.class);

            log.info("받은 응답: {}", response);
            log.info("==== Python 통신 테스트 완료 ====");

            return response;
        } catch (ResourceAccessException e) {
            log.error("Python 서비스 연결 실패: {}", e.getMessage());
            throw RecommendationException.aiServiceUnavailable();
        }
    }
}
