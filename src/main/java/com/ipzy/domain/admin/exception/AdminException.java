package com.ipzy.domain.admin.exception;

import com.ipzy._global.exception.BusinessException;

/**
 * 관리자 관련 예외 (팩토리 메서드로 생성)
 */
public class AdminException extends BusinessException {

    private AdminException(AdminErrorCode errorCode) {
        super(errorCode);
    }

    private AdminException(AdminErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static AdminException invalidCredentials() {
        return new AdminException(AdminErrorCode.INVALID_CREDENTIALS);
    }

    public static AdminException notAdminUser() {
        return new AdminException(AdminErrorCode.NOT_ADMIN_USER);
    }

    public static AdminException adminNotFound() {
        return new AdminException(AdminErrorCode.ADMIN_NOT_FOUND);
    }

    public static AdminException accountSuspended() {
        return new AdminException(AdminErrorCode.ACCOUNT_SUSPENDED);
    }

    public static AdminException sessionRequired() {
        return new AdminException(AdminErrorCode.SESSION_REQUIRED);
    }

    public static AdminException userNotFound() {
        return new AdminException(AdminErrorCode.USER_NOT_FOUND);
    }

    public static AdminException invalidStatusChange() {
        return new AdminException(AdminErrorCode.INVALID_STATUS_CHANGE);
    }
}
