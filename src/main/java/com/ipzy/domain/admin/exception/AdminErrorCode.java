package com.ipzy.domain.admin.exception;

import com.ipzy._global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdminErrorCode implements ErrorCode {

    // 인증 관련
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "ADMIN_001", "이메일 또는 비밀번호가 올바르지 않습니다"),
    NOT_ADMIN_USER(HttpStatus.FORBIDDEN, "ADMIN_002", "관리자 권한이 없습니다"),
    ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN_003", "관리자를 찾을 수 없습니다"),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "ADMIN_004", "정지된 계정입니다"),
    SESSION_REQUIRED(HttpStatus.UNAUTHORIZED, "ADMIN_005", "로그인이 필요합니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
