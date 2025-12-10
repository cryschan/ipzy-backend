package com.ipzy.domain.subscription.service;

import com.ipzy.domain.subscription.dto.Response.SubscriptionPlanResponse;
import com.ipzy.domain.subscription.entity.SubscriptionPlan;
import com.ipzy.domain.subscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    /**
     * 메서드 목적: 전체 플랜 목록 조회
     * 예상 반환값: free, basic, pro
     */
    public List<SubscriptionPlanResponse> findAll() {
        return subscriptionPlanRepository.findAll()
                .stream()
                .map(SubscriptionPlanResponse::from)
                .toList();
    }

    /**
     * 메서드 목적: 플랜 이름별 조회
     */
    public SubscriptionPlanResponse findByName(String name) {
        SubscriptionPlan entity = subscriptionPlanRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 플랜입니다."));
        return SubscriptionPlanResponse.from(entity);
    }
}
