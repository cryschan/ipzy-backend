package com.ipzy.domain.outfit.exception;

import com.ipzy.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 코디 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum OutfitErrorCode implements ErrorCode {

    OUTFIT_NOT_FOUND(HttpStatus.NOT_FOUND, "OUTFIT_001", "코디를 찾을 수 없습니다"),
    DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "OUTFIT_002", "일일 추천 횟수를 초과했습니다"),
    OUTFIT_SAVE_FAILED(HttpStatus.BAD_REQUEST, "OUTFIT_003", "코디 저장에 실패했습니다"),
    OUTFIT_ALREADY_SAVED(HttpStatus.CONFLICT, "OUTFIT_004", "이미 저장된 코디입니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
