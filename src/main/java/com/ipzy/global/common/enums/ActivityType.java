package com.ipzy.global.common.enums;

/**
 * 사용자 활동 유형
 */
public enum ActivityType {
    /** 로그인 */
    LOGIN,
    /** 로그아웃 */
    LOGOUT,
    /** 퀴즈 시작 */
    QUIZ_START,
    /** 퀴즈 완료 */
    QUIZ_COMPLETE,
    /** 추천 코디 조회 */
    RECOMMENDATION_VIEW,
    /** 추천 코디 저장 */
    RECOMMENDATION_SAVE,
    /** 상품 상세 조회 */
    PRODUCT_VIEW,
    /** 상품 외부 링크 클릭 (네이버 쇼핑 이동) */
    PRODUCT_CLICK,
    /** 구독 시작 */
    SUBSCRIPTION_START,
    /** 구독 취소 */
    SUBSCRIPTION_CANCEL,
    /** 프로필 수정 */
    PROFILE_UPDATE,
    /** 비밀번호 변경 */
    PASSWORD_CHANGE
}
