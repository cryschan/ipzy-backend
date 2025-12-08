package com.ipzy.domain.recommendation.exception;

import com.ipzy.global.exception.BusinessException;

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
}
