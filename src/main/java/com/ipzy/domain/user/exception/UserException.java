package com.ipzy.domain.user.exception;

import com.ipzy._global.exception.BusinessException;
import com.ipzy._global.exception.ErrorCode;

/**
 * 사용자 관련 예외 (팩토리 메서드로 생성)
 */
public class UserException extends BusinessException {

    private UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    private UserException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static UserException notFound() {
        return new UserException(UserErrorCode.USER_NOT_FOUND);
    }

    public static UserException notFound(Long userId) {
        return new UserException(UserErrorCode.USER_NOT_FOUND, "ID: " + userId);
    }

    public static UserException profileUpdateFailed() {
        return new UserException(UserErrorCode.PROFILE_UPDATE_FAILED);
    }

    public static UserException alreadyDeleted() {
        return new UserException(UserErrorCode.ALREADY_DELETED);
    }

    public static UserException adminCannotWithdraw() {
        return new UserException(UserErrorCode.ADMIN_CANNOT_WITHDRAW);
    }
}
