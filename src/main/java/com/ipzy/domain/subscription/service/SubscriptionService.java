package com.ipzy.domain.subscription.service;

import com.ipzy.domain.subscription.dto.Response.SubscriptionPlanResponse;
import com.ipzy.domain.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 구독 플랜 <-> 사용자 간의 연결을 위한 Service
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    //private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanService subscriptionPlanService;

    /**
     * 전체 구독 플랜 목록 조회
     * @return 모든 플랜 목록 (Free, Basic, Pro)
     */
    public List<SubscriptionPlanResponse> findAllPlans() {
        return subscriptionPlanService.findAll();
    }

    /**
     * 플랜 이름으로 조회
     * @param name 플랜 이름 (예: "FREE", "BASIC", "PRO")
     * @return 플랜 정보
     */
    public SubscriptionPlanResponse findPlanByName(String name) {
        return subscriptionPlanService.findByName(name);
    }

}
