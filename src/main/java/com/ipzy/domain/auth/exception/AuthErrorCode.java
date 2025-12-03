package com.ipzy.domain.auth.exception;

import com.ipzy.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_001", "유효하지 않은 이메일 또는 비밀번호입니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_002", "토큰이 만료되었습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "토큰이 유효하지 않습니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_004", "리프레시 토큰이 유효하지 않습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_005", "이미 등록된 이메일입니다"),
    ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "AUTH_006", "관리자 권한이 필요합니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
