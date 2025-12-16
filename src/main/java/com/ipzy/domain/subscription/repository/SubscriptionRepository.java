package com.ipzy.domain.subscription.repository;

import com.ipzy._global.common.enums.SubscriptionStatus;
import com.ipzy.domain.subscription.entity.Subscription;
import com.ipzy.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * 사용자의 활성 구독 조회
     */
    Optional<Subscription> findByUserAndStatus(User user, SubscriptionStatus status);

    /**
     * 사용자의 최신 구독 조회 (모든 상태 포함)
     */
    Optional<Subscription> findTopByUserOrderByCreatedAtDesc(User user);

    /**
     * ⭐ 비관적 락을 사용한 사용자의 유효한 구독 조회 (ACTIVE 또는 PENDING)
     * SELECT ... FOR UPDATE를 통해 동시성 문제 해결
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s " +
           "WHERE s.user = :user " +
           "AND s.status IN :statuses " +
           "ORDER BY s.createdAt DESC " +
           "LIMIT 1")
    Optional<Subscription> findValidSubscriptionWithLock(
        @Param("user") User user,
        @Param("statuses") Set<SubscriptionStatus> statuses
    );

    /**
     * 사용자의 유효한 구독 조회 (ACTIVE 또는 PENDING)
     */
    @Query("SELECT s FROM Subscription s " +
           "WHERE s.user = :user " +
           "AND s.status IN :statuses " +
           "ORDER BY s.createdAt DESC " +
           "LIMIT 1")
    Optional<Subscription> findValidSubscription(
        @Param("user") User user,
        @Param("statuses") Set<SubscriptionStatus> statuses
    );

    /**
     * 사용자의 모든 구독 히스토리 조회
     */
    List<Subscription> findAllByUserOrderByCreatedAtDesc(User user);

    /**
     * 특정 상태의 구독 개수 조회
     */
    @Query("SELECT COUNT(s) FROM Subscription s " +
           "WHERE s.user = :user " +
           "AND s.status IN :statuses")
    long countByUserAndStatusIn(
        @Param("user") User user,
        @Param("statuses") Set<SubscriptionStatus> statuses
    );
}
