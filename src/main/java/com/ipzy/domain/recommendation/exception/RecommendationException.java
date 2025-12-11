package com.ipzy.domain.recommendation.exception;

import com.ipzy._global.exception.BusinessException;

/**
 * 추천 도메인 예외
 */
public class RecommendationException extends BusinessException {

    public RecommendationException(RecommendationErrorCode errorCode) {
        super(errorCode);
    }

    public RecommendationException(RecommendationErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    // Static factory methods
    public static RecommendationException sessionNotFound(Long sessionId) {
        return new RecommendationException(
                RecommendationErrorCode.SESSION_NOT_FOUND,
                "세션 ID: " + sessionId
        );
    }

    public static RecommendationException sessionNotCompleted(Long sessionId) {
        return new RecommendationException(
                RecommendationErrorCode.SESSION_NOT_COMPLETED,
                "세션 ID: " + sessionId
        );
    }

    public static RecommendationException recommendationAlreadyExists(Long sessionId) {
        return new RecommendationException(
                RecommendationErrorCode.RECOMMENDATION_ALREADY_EXISTS,
                "세션 ID: " + sessionId
        );
    }

    public static RecommendationException aiServiceUnavailable() {
        return new RecommendationException(RecommendationErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    public static RecommendationException aiInvalidResponse() {
        return new RecommendationException(RecommendationErrorCode.AI_INVALID_RESPONSE);
    }

    public static RecommendationException productNotFound(Long productId) {
        return new RecommendationException(
                RecommendationErrorCode.PRODUCT_NOT_FOUND_IN_RECOMMENDATION,
                "상품 ID: " + productId
        );
    }

    public static RecommendationException accessDenied(Long sessionId) {
        return new RecommendationException(
                RecommendationErrorCode.ACCESS_DENIED,
                "세션 ID: " + sessionId
        );
    }

    public static RecommendationException emptyRecommendation() {
        return new RecommendationException(RecommendationErrorCode.EMPTY_RECOMMENDATION);
    }
}
