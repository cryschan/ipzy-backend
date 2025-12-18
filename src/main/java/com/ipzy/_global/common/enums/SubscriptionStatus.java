package com.ipzy._global.common.enums;

import java.util.Set;

public enum SubscriptionStatus {
    ACTIVE,     // 활성 구독
    PENDING,    // 결제 대기
    EXPIRED,    // 만료
    CANCELLED;  // 취소

    /**
     * Unique Constraint 대상 상태들
     * 사용자당 하나만 가질 수 있는 상태 (ACTIVE 또는 PENDING)
     */
    public static final Set<SubscriptionStatus> UNIQUE_CONSTRAINT_STATUSES =
        Set.of(ACTIVE, PENDING);

    /**
     * 이 상태가 Unique Constraint 대상인지 확인
     * (사용자당 하나만 가질 수 있는 상태)
     */
    public boolean isUniquePerUser() {
        return UNIQUE_CONSTRAINT_STATUSES.contains(this);
    }

    /**
     * 활성 상태인지 확인
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * 결제 대기 중인지 확인
     */
    public boolean isPending() {
        return this == PENDING;
    }

    /**
     * 종료된 상태인지 확인 (EXPIRED 또는 CANCELLED)
     */
    public boolean isTerminated() {
        return this == EXPIRED || this == CANCELLED;
    }

    /**
     * 유효한 구독인지 확인 (ACTIVE 또는 PENDING)
     */
    public boolean isValid() {
        return isUniquePerUser();
    }
}
