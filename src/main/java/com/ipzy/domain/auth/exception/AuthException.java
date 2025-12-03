package com.ipzy.domain.auth.exception;

import com.ipzy.global.exception.BusinessException;

public class AuthException extends BusinessException {

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }

    public AuthException(AuthErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
