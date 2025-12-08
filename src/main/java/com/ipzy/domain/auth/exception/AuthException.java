package com.ipzy.domain.auth.exception;

import com.ipzy._global.exception.BusinessException;

/**
 * 인증 관련 예외 (팩토리 메서드로 생성)
 */
public class AuthException extends BusinessException {

    private AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }

    private AuthException(AuthErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static AuthException unauthorized() {
        return new AuthException(AuthErrorCode.UNAUTHORIZED);
    }

    public static AuthException oauthFailed() {
        return new AuthException(AuthErrorCode.OAUTH_FAILED);
    }

    public static AuthException oauthFailed(String reason) {
        return new AuthException(AuthErrorCode.OAUTH_FAILED,
                "OAuth 인증에 실패했습니다: " + reason);
    }

    public static AuthException adminRequired() {
        return new AuthException(AuthErrorCode.ADMIN_REQUIRED);
    }

    public static AuthException duplicateEmail() {
        return new AuthException(AuthErrorCode.DUPLICATE_EMAIL);
    }

    public static AuthException sessionExpired() {
        return new AuthException(AuthErrorCode.SESSION_EXPIRED);
    }

    public static AuthException oauthAccessDenied() {
        return new AuthException(AuthErrorCode.OAUTH_ACCESS_DENIED);
    }

    public static AuthException oauthInvalidToken() {
        return new AuthException(AuthErrorCode.OAUTH_INVALID_TOKEN);
    }

    public static AuthException oauthInvalidResponse() {
        return new AuthException(AuthErrorCode.OAUTH_INVALID_RESPONSE);
    }

    public static AuthException oauthInvalidResponse(String reason) {
        return new AuthException(AuthErrorCode.OAUTH_INVALID_RESPONSE,
                "OAuth 응답을 처리할 수 없습니다: " + reason);
    }
}
