package com.ipzy.domain.recommendation.client;

import com.ipzy.domain.recommendation.dto.request.RecommendationRequest;
import com.ipzy.domain.recommendation.dto.response.RecommendationResponse;
import com.ipzy.domain.recommendation.exception.RecommendationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;

/**
 * Python AI 서비스 통신 클라이언트
 * <p>
 * Python FastAPI와 HTTP 통신을 수행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PythonAiClient implements AiRecommendationClient {

    private final RestClient pythonAiRestClient;

    @Override
    public RecommendationResponse requestRecommendation(RecommendationRequest request) {
        log.info("==== Python AI 추천 요청 시작 ====");
        log.info("세션 ID: {}, 답변 수: {}", request.sessionId(), request.answers().size());

        try {
            RecommendationResponse response = pythonAiRestClient.post()
                    .uri("/api/recommend")
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
            // 타임아웃과 연결 실패 분리
            if (e.getCause() instanceof SocketTimeoutException) {
                log.error("Python AI 서비스 타임아웃: {}", e.getMessage());
                throw RecommendationException.aiRequestTimeout();
            }
            log.error("Python AI 서비스 연결 실패: {}", e.getMessage());
            throw RecommendationException.aiServiceUnavailable();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Python AI 서비스 HTTP 에러: {} - {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw RecommendationException.aiInvalidResponse();
        } catch (RestClientResponseException e) {
            log.error("Python AI 서비스 응답 에러: {}", e.getMessage());
            throw RecommendationException.aiInvalidResponse();
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
            // 타임아웃과 연결 실패 분리
            if (e.getCause() instanceof SocketTimeoutException) {
                log.error("Python 서비스 타임아웃: {}", e.getMessage());
                throw RecommendationException.aiRequestTimeout();
            }
            log.error("Python 서비스 연결 실패: {}", e.getMessage());
            throw RecommendationException.aiServiceUnavailable();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Python 서비스 HTTP 에러: {} - {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw RecommendationException.aiInvalidResponse();
        } catch (RestClientResponseException e) {
            log.error("Python 서비스 응답 에러: {}", e.getMessage());
            throw RecommendationException.aiInvalidResponse();
        }
    }
}
