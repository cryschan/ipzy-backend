package com.ipzy.domain.subscription.exception;

import com.ipzy._global.exception.BusinessException;

/**
 * 구독 관련 예외 (팩토리 메서드로 생성)
 */
public class SubscriptionException extends BusinessException {

    private SubscriptionException(SubscriptionErrorCode errorCode) {
        super(errorCode);
    }

    private SubscriptionException(SubscriptionErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    // ========== 플랜 관련 예외 ==========

    public static SubscriptionException planNotFound() {
        return new SubscriptionException(SubscriptionErrorCode.PLAN_NOT_FOUND);
    }

    public static SubscriptionException planNotFound(Long planId) {
        return new SubscriptionException(
                SubscriptionErrorCode.PLAN_NOT_FOUND,
                "플랜을 찾을 수 없습니다 (ID: " + planId + ")"
        );
    }

    public static SubscriptionException planNotFoundByName(String planName) {
        return new SubscriptionException(
                SubscriptionErrorCode.PLAN_NOT_FOUND,
                "플랜을 찾을 수 없습니다 (이름: " + planName + ")"
        );
    }

    public static SubscriptionException noPlansAvailable() {
        return new SubscriptionException(SubscriptionErrorCode.NO_PLANS_AVAILABLE);
    }

    public static SubscriptionException planAlreadyExists(String planName) {
        return new SubscriptionException(
                SubscriptionErrorCode.PLAN_ALREADY_EXISTS,
                "이미 존재하는 플랜 이름입니다: " + planName
        );
    }

    // ========== 구독 관련 예외 ==========

    public static SubscriptionException subscriptionNotFound() {
        return new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
    }

    public static SubscriptionException subscriptionNotFound(Long userId) {
        return new SubscriptionException(
                SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND,
                "활성 구독이 없습니다 (User ID: " + userId + ")"
        );
    }

    public static SubscriptionException duplicateSubscription() {
        return new SubscriptionException(SubscriptionErrorCode.DUPLICATE_SUBSCRIPTION);
    }

    public static SubscriptionException subscriptionExpired() {
        return new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_EXPIRED);
    }

    public static SubscriptionException subscriptionCancelled() {
        return new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_CANCELLED);
    }

    public static SubscriptionException paymentPending(String message) {
        return new SubscriptionException(
                SubscriptionErrorCode.PAYMENT_REQUIRED,
                message
        );
    }

    // ========== 결제 관련 예외 ==========

    public static SubscriptionException paymentRequired() {
        return new SubscriptionException(SubscriptionErrorCode.PAYMENT_REQUIRED);
    }

    public static SubscriptionException paymentRequired(String planName) {
        return new SubscriptionException(
                SubscriptionErrorCode.PAYMENT_REQUIRED,
                planName + " 플랜은 결제가 필요합니다"
        );
    }

    public static SubscriptionException paymentFailed() {
        return new SubscriptionException(SubscriptionErrorCode.PAYMENT_FAILED);
    }

    public static SubscriptionException paymentFailed(String reason) {
        return new SubscriptionException(
                SubscriptionErrorCode.PAYMENT_FAILED,
                "결제 처리에 실패했습니다: " + reason
        );
    }
}