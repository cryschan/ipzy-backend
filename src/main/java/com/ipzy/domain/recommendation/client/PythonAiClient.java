package com.ipzy.domain.recommendation.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Python AI 서비스와 통신하는 클라이언트
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PythonAiClient {

    private final RestClient pythonAiRestClient;

    /**
     * 통신 테스트용 메서드
     * - Python API 준비 전 통신 흐름 확인용
     * - 사용 예시: GET /api/recommendations/test?msg=hello
     */
    public String testConnection(String msg) {
        log.info("==== Python 통신 테스트 시작 ====");
        log.info("보내는 메시지: {}", msg);

        // TODO: Python API 준비되면 아래 주석 해제
        // String response = pythonAiRestClient.post()
        //         .uri("/test")
        //         .body(msg)
        //         .retrieve()
        //         .body(String.class);
        // return response;

        // 임시 Mock 응답
        String mockResponse = "Python 응답 (Mock): " + msg;
        log.info("받은 응답: {}", mockResponse);
        log.info("==== Python 통신 테스트 완료 ====");

        return mockResponse;
    }
}
