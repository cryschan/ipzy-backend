package com.ipzy.domain.user.exception;

import com.ipzy._global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 사용자 관련 에러 코드 (USER_001 ~ USER_003)
 */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다"),
    PROFILE_UPDATE_FAILED(HttpStatus.BAD_REQUEST, "USER_002", "프로필 업데이트에 실패했습니다"),
    ALREADY_DELETED(HttpStatus.BAD_REQUEST, "USER_003", "이미 탈퇴한 사용자입니다"),
    ADMIN_CANNOT_WITHDRAW(HttpStatus.BAD_REQUEST, "USER_004", "관리자는 일반 탈퇴할 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
