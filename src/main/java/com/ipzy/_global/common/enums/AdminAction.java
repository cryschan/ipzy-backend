package com.ipzy._global.common.enums;

/**
 * 관리자 감사 로그 액션 유형
 */
public enum AdminAction {
    /** 사용자 정보 조회 */
    USER_VIEW,
    /** 사용자 정보 수정 */
    USER_UPDATE,
    /** 사용자 계정 정지 */
    USER_SUSPEND,
    /** 사용자 계정 활성화 */
    USER_ACTIVATE,
    /** 사용자 계정 삭제 */
    USER_DELETE,
    /** 상품 등록 */
    PRODUCT_CREATE,
    /** 상품 수정 */
    PRODUCT_UPDATE,
    /** 상품 삭제 */
    PRODUCT_DELETE,
    /** 구독 정보 수정 */
    SUBSCRIPTION_UPDATE,
    /** 구독 강제 취소 */
    SUBSCRIPTION_CANCEL,
    /** 결제 내역 조회 */
    PAYMENT_VIEW,
    /** 결제 환불 처리 */
    PAYMENT_REFUND,
    /** 퀴즈 생성 */
    QUIZ_CREATE,
    /** 퀴즈 수정 */
    QUIZ_UPDATE,
    /** 퀴즈 삭제 */
    QUIZ_DELETE,
    /** 시스템 설정 변경 */
    SYSTEM_CONFIG_UPDATE
}
