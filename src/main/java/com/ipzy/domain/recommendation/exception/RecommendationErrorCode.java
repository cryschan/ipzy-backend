package com.ipzy.domain.recommendation.exception;

import com.ipzy.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 추천 도메인 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum RecommendationErrorCode implements ErrorCode {

    // 추천 관련
    RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "REC001", "추천 정보를 찾을 수 없습니다"),

    // AI 서비스 관련
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "REC101", "AI 서비스에 연결할 수 없습니다"),
    AI_REQUEST_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "REC102", "AI 서비스 응답 시간이 초과되었습니다"),
    AI_INVALID_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "REC103", "AI 서비스 응답을 처리할 수 없습니다"),

    // 상품 관련
    PRODUCT_NOT_FOUND_IN_RECOMMENDATION(HttpStatus.BAD_REQUEST, "REC201", "추천된 상품을 찾을 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
