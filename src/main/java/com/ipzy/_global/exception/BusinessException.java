package com.ipzy._global.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 예외의 기본 클래스 - 모든 도메인 예외가 상속
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * ErrorCode 메시지에 추가 정보를 덧붙여 생성
     * 결과: "ErrorCode 메시지 (추가 정보)"
     */
    public BusinessException(ErrorCode errorCode, String additionalInfo) {
        super(errorCode.getMessage() + " (" + additionalInfo + ")");
        this.errorCode = errorCode;
    }
}
