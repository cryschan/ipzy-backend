package com.ipzy.domain.subscription.service;

import com.ipzy.domain.subscription.dto.Response.SubscriptionPlanResponse;
import com.ipzy.domain.subscription.entity.SubscriptionPlan;
import com.ipzy.domain.subscription.exception.SubscriptionException;
import com.ipzy.domain.subscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    /**
     * 전체 플랜 목록 조회
     * @return 모든 플랜 목록 (Free, Basic, Pro)
     * @throws SubscriptionException 등록된 플랜이 없을 경우
     */
    public List<SubscriptionPlanResponse> findAll() {
        List<SubscriptionPlan> plans = subscriptionPlanRepository.findAll();

        if (plans.isEmpty()) {
            throw SubscriptionException.noPlansAvailable();
        }

        return plans.stream()
                .map(SubscriptionPlanResponse::from)
                .toList();
    }

    /**
     * 플랜 이름으로 조회
     * @param name 플랜 이름 (FREE, BASIC, PRO)
     * @return 플랜 정보
     * @throws SubscriptionException 플랜을 찾을 수 없을 경우
     */
    public SubscriptionPlanResponse findByName(String name) {
        SubscriptionPlan entity = subscriptionPlanRepository.findByName(name)
                .orElseThrow(() -> SubscriptionException.planNotFoundByName(name));
        return SubscriptionPlanResponse.from(entity);
    }

    /**
     * 플랜 ID로 Entity 조회 (내부용)
     * @param planId 플랜 ID
     * @return SubscriptionPlan 엔티티
     * @throws SubscriptionException 플랜을 찾을 수 없을 경우
     */
    public SubscriptionPlan findEntityById(Long planId) {
        return subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> SubscriptionException.planNotFound(planId));
    }

    /**
     * 플랜 이름으로 Entity 조회 (내부용)
     * @param name 플랜 이름
     * @return SubscriptionPlan 엔티티
     * @throws SubscriptionException 플랜을 찾을 수 없을 경우
     */
    public SubscriptionPlan findByNameEntity(String name) {
        return subscriptionPlanRepository.findByName(name)
                .orElseThrow(() -> SubscriptionException.planNotFoundByName(name));
    }
}
