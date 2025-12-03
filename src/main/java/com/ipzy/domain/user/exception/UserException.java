package com.ipzy.domain.user.exception;

import com.ipzy.global.exception.BusinessException;
import com.ipzy.global.exception.ErrorCode;

public class UserException extends BusinessException {

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UserException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
