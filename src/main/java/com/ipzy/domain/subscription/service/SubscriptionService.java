package com.ipzy.domain.subscription.service;

import com.ipzy._global.common.enums.SubscriptionStatus;
import com.ipzy.domain.subscription.dto.Request.CreateSubscriptionRequest;
import com.ipzy.domain.subscription.dto.Response.SubscriptionPlanResponse;
import com.ipzy.domain.subscription.dto.Response.SubscriptionResponse;
import com.ipzy.domain.subscription.entity.Subscription;
import com.ipzy.domain.subscription.entity.SubscriptionPlan;
import com.ipzy.domain.subscription.exception.SubscriptionException;
import com.ipzy.domain.subscription.repository.SubscriptionRepository;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.exception.UserException;
import com.ipzy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 기능
 * 1. 구독 생성/조회/취소
 * 2. 구독 상태 변경
 *
 */
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class SubscriptionService {

    private static final String DEFAULT_FREE_PLAN_NAME = "FREE";
    private static final int FREE_TRIAL_DAYS = 7;

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanService subscriptionPlanService;
    private final UserRepository userRepository;

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

    /**
     * 내 구독 조회
     * @param userId 사용자 ID
     * @return 구독 정보 (상태 무관: FREE, ACTIVE, EXPIRED 등 모두 조회 가능)
     */
    @Transactional
    public SubscriptionResponse getMySubscription(Long userId) {
        Subscription subscription = ensureDefaultSubscription(userId);
        return SubscriptionResponse.from(subscription);
    }

    /**
     * 선택된 플랜의 종료일을 계산합니다.
     */
    private LocalDateTime calculateEndDate(SubscriptionPlan plan, LocalDateTime startDate) {
        if (plan.getPrice() == 0) {
            return startDate.plusDays(FREE_TRIAL_DAYS);
        }

        if (plan.getBillingPeriod() == null) {
            return startDate.plusMonths(1);
        }

        return switch (plan.getBillingPeriod()) {
            case MONTHLY -> startDate.plusMonths(1);
            case YEARLY -> startDate.plusYears(1);
        };
    }

    /**
     * 구독 생성
     * @param userId 사용자 ID
     * @param request 구독 생성 요청
     * @return 생성된 구독 정보
     * @throws SubscriptionException 중복 구독이 있거나 플랜을 찾을 수 없을 경우
     */
    @Transactional
    public SubscriptionResponse createSubscription(Long userId, CreateSubscriptionRequest request) {
        User user = findUserById(userId);
        Subscription subscription = getOrCreateSubscription(user);
        expireIfNeeded(subscription);

        SubscriptionPlan plan = subscriptionPlanService.findEntityById(request.planId());
        LocalDateTime now = LocalDateTime.now();
        SubscriptionStatus nextStatus = plan.getPrice() > 0
                ? SubscriptionStatus.PENDING
                : SubscriptionStatus.ACTIVE;

        subscription.updatePlan(
                plan,
                nextStatus,
                now,
                calculateEndDate(plan, now),
                plan.getPrice() > 0
        );

        return SubscriptionResponse.from(subscription);
    }

    /**
     * 구독 취소
     * @param userId 사용자 ID
     * @param reason 취소 사유
     * @return 취소된 구독 정보
     * @throws SubscriptionException 활성 구독이 없을 경우
     */
    @Transactional
    public SubscriptionResponse cancelSubscription(Long userId, String reason) {
        Subscription subscription = findActiveSubscription(userId);
        subscription.cancel(reason);
        return SubscriptionResponse.from(subscription);
    }

    /**
     * 사용자 조회 (내부용)
     * @param userId 사용자 ID
     * @return User 엔티티
     * @throws UserException 사용자를 찾을 수 없을 경우
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> UserException.notFound(userId));
    }

    /**
     * 사용자의 구독을 조회하거나 생성 (상태 무관)
     * - FREE, EXPIRED, ACTIVE 등 모든 상태 허용
     * - expireIfNeeded로 자동 만료 처리
     * - 구독이 없으면 FREE 플랜으로 자동 생성
     *
     * @param userId 사용자 ID
     * @return 구독 엔티티 (상태 무관)
     */
    @Transactional
    public Subscription ensureDefaultSubscription(Long userId) {
        User user = findUserById(userId);
        Subscription subscription = getOrCreateSubscription(user);
        expireIfNeeded(subscription);
        return subscription;
    }

    /**
     * ACTIVE 상태의 구독만 조회 (엄격한 검증)
     * - 구독 취소 등 ACTIVE 상태가 필수인 작업에만 사용
     * - FREE, EXPIRED 상태는 예외 발생
     *
     * @param userId 사용자 ID
     * @return ACTIVE 상태의 구독 엔티티
     * @throws SubscriptionException 활성 구독이 없을 경우
     */
    private Subscription findActiveSubscription(Long userId) {
        Subscription subscription = ensureDefaultSubscription(userId);

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw SubscriptionException.subscriptionNotFound(userId);
        }

        return subscription;
    }

    private Subscription getOrCreateSubscription(User user) {
        Optional<Subscription> latest = subscriptionRepository.findTopByUserOrderByCreatedAtDesc(user);
        return latest.orElseGet(() -> startFreeSubscription(user));
    }

    private Subscription startFreeSubscription(User user) {
        SubscriptionPlan freePlan = subscriptionPlanService.findEntityByName(DEFAULT_FREE_PLAN_NAME);
        LocalDateTime now = LocalDateTime.now();

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(freePlan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .endDate(now.plusDays(FREE_TRIAL_DAYS))
                .autoRenew(false)
                .build();

        return subscriptionRepository.save(subscription);
    }

    private void expireIfNeeded(Subscription subscription) {
        if (subscription == null) {
            return;
        }

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE
                && LocalDateTime.now().isAfter(subscription.getEndDate())) {
            subscription.expire();
        }
    }

}
