package com.ipzy.domain.subscription.repository;

import com.ipzy.domain.subscription.entity.Subscription;
import com.ipzy.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * 사용자의 최신 구독 조회 (createdAt 기준)
     */
    Optional<Subscription> findTopByUserOrderByCreatedAtDesc(User user);
}
