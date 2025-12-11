package com.ipzy.domain.subscription.exception;

import com.ipzy._global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 구독 관련 에러 코드 (SUBSCRIPTION_001 ~ SUBSCRIPTION_009)
 */
@Getter
@RequiredArgsConstructor
public enum SubscriptionErrorCode implements ErrorCode {

    // 플랜 관련 (001 ~ 003)
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "SUBSCRIPTION_001", "구독 플랜을 찾을 수 없습니다"),
    NO_PLANS_AVAILABLE(HttpStatus.NOT_FOUND, "SUBSCRIPTION_002", "등록된 구독 플랜이 없습니다"),
    PLAN_ALREADY_EXISTS(HttpStatus.CONFLICT, "SUBSCRIPTION_003", "이미 존재하는 플랜 이름입니다"),

    // 구독 관련 (004 ~ 007)
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SUBSCRIPTION_004", "활성 구독이 없습니다"),
    DUPLICATE_SUBSCRIPTION(HttpStatus.BAD_REQUEST, "SUBSCRIPTION_005", "이미 활성 구독이 존재합니다"),
    SUBSCRIPTION_EXPIRED(HttpStatus.BAD_REQUEST, "SUBSCRIPTION_006", "구독이 만료되었습니다"),
    SUBSCRIPTION_CANCELLED(HttpStatus.BAD_REQUEST, "SUBSCRIPTION_007", "이미 취소된 구독입니다"),

    // 결제 관련 (008 ~ 009)
    PAYMENT_REQUIRED(HttpStatus.PAYMENT_REQUIRED, "SUBSCRIPTION_008", "결제가 필요한 플랜입니다"),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "SUBSCRIPTION_009", "결제 처리에 실패했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}