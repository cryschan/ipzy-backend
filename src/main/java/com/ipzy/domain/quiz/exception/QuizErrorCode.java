package com.ipzy.domain.quiz.exception;

import com.ipzy._global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 퀴즈 관련 에러 코드 (QUIZ_001 ~ QUIZ_004)
 */
@Getter
@RequiredArgsConstructor
public enum QuizErrorCode implements ErrorCode {

    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "QUIZ_001", "퀴즈를 찾을 수 없습니다"),
    INVALID_QUIZ_RESPONSE(HttpStatus.BAD_REQUEST, "QUIZ_002", "유효하지 않은 퀴즈 응답입니다"),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "QUIZ_003", "퀴즈 세션을 찾을 수 없습니다"),
    SESSION_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "QUIZ_004", "이미 완료된 퀴즈 세션입니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
