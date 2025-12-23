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
    SESSION_REQUIRED(HttpStatus.UNAUTHORIZED, "ADMIN_005", "로그인이 필요합니다"),

    // 회원 관리 관련
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN_006", "회원을 찾을 수 없습니다"),
    INVALID_STATUS_CHANGE(HttpStatus.BAD_REQUEST, "ADMIN_007", "유효하지 않은 상태 변경입니다"),

    // 구독 관리 관련
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN_010", "구독을 찾을 수 없습니다"),
    INVALID_SUBSCRIPTION_CANCEL(HttpStatus.BAD_REQUEST, "ADMIN_011", "활성화된 구독만 취소할 수 있습니다"),

    // 상품 관리 관련
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN_012", "상품을 찾을 수 없습니다"),
    PRODUCT_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "ADMIN_013", "삭제된 상품은 수정할 수 없습니다"),

    // 퀴즈 관리 관련
    QUIZ_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN_014", "퀴즈 세션을 찾을 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
