package com.ipzy.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 인터페이스 - 모든 에러 코드 enum이 구현
 */
public interface ErrorCode {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}
