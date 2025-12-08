package com.ipzy.domain.auth.exception;

import com.ipzy.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 인증 관련 에러 코드 (AUTH_001 ~ AUTH_008)
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다"),
    OAUTH_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_002", "OAuth 인증에 실패했습니다"),
    ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "AUTH_003", "관리자 권한이 필요합니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_004", "이미 사용 중인 이메일입니다"),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_005", "세션이 만료되었습니다"),
    OAUTH_ACCESS_DENIED(HttpStatus.UNAUTHORIZED, "AUTH_006", "사용자가 로그인을 취소했습니다"),
    OAUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_007", "OAuth 토큰이 유효하지 않습니다"),
    OAUTH_INVALID_RESPONSE(HttpStatus.UNAUTHORIZED, "AUTH_008", "OAuth 응답을 처리할 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
