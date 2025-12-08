package com.ipzy.domain.user.exception;

import com.ipzy.global.exception.ErrorCode;
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
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "USER_003", "비밀번호가 일치하지 않습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
