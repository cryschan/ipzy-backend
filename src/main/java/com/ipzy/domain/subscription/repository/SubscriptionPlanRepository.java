package com.ipzy.domain.subscription.repository;

import com.ipzy.domain.subscription.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    /**
     * 플랜 이름으로 조회
     */
    Optional<SubscriptionPlan> findByName(String name);
}