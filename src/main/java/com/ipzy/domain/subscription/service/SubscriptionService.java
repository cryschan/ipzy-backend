package com.ipzy.domain.subscription.service;

import com.ipzy.domain.subscription.dto.Request.CreateSubscriptionRequest;
import com.ipzy.domain.subscription.dto.Response.SubscriptionPlanResponse;
import com.ipzy.domain.subscription.dto.Response.SubscriptionResponse;
import com.ipzy.domain.subscription.entity.Subscription;
import com.ipzy.domain.subscription.entity.SubscriptionPlan;
import com.ipzy.domain.subscription.exception.SubscriptionException;
import com.ipzy.domain.subscription.repository.SubscriptionRepository;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.repository.UserRepository;
import com.ipzy._global.common.enums.BillingPeriod;
import com.ipzy._global.common.enums.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 구독 플랜 <-> 사용자 간의 연결을 위한 Service
 * 비관적 락을 사용하여 동시성 문제 해결
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private static final String DEFAULT_FREE_PLAN_NAME = "FREE";

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
     * 사용자의 유효한 구독 조회 (ACTIVE 또는 PENDING)
     * @param userId 사용자 ID
     * @return 구독 정보
     */
    public SubscriptionResponse getMySubscription(Long userId) {
        User user = findUserById(userId);

        Subscription subscription = subscriptionRepository
                .findValidSubscription(user, SubscriptionStatus.UNIQUE_CONSTRAINT_STATUSES)
                .orElseThrow(() -> SubscriptionException.subscriptionNotFound(userId));

        // 만료 확인 및 처리
        expireIfNeeded(subscription);

        return SubscriptionResponse.from(subscription);
    }

    /**
     * 사용자의 구독을 조회하거나 생성 (비관적 락 사용)
     *
     * 동시성 문제 해결:
     * 1. 비관적 락(PESSIMISTIC_WRITE)으로 동시 접근 제어
     * 2. SELECT ... FOR UPDATE로 다른 트랜잭션의 읽기/쓰기 차단
     * 3. 트랜잭션 내에서 안전하게 구독 생성
     */
    @Transactional
    public Subscription ensureDefaultSubscription(Long userId) {
        User user = findUserById(userId);

        // ⭐ 비관적 락을 사용하여 유효한 구독 조회
        Optional<Subscription> validSubscription =
            subscriptionRepository.findValidSubscriptionWithLock(
                user,
                SubscriptionStatus.UNIQUE_CONSTRAINT_STATUSES
            );

        if (validSubscription.isPresent()) {
            Subscription subscription = validSubscription.get();
            expireIfNeeded(subscription);
            return subscription;
        }

        // 락을 획득한 상태에서 구독이 없으면 FREE 플랜 생성
        // 다른 트랜잭션은 락이 해제될 때까지 대기하므로 중복 생성 방지
        log.info("사용자의 유효한 구독이 없어 FREE 플랜 생성: userId={}", userId);
        return startFreeSubscription(user);
    }

    /**
     * 구독 생성/변경 (비관적 락 사용)
     */
    @Transactional
    public SubscriptionResponse createSubscription(Long userId, CreateSubscriptionRequest request) {
        User user = findUserById(userId);

        // ⭐ 비관적 락을 사용하여 유효한 구독 조회 (동시성 제어)
        Optional<Subscription> existingValid =
            subscriptionRepository.findValidSubscriptionWithLock(
                user,
                SubscriptionStatus.UNIQUE_CONSTRAINT_STATUSES
            );

        Subscription subscription;

        if (existingValid.isPresent()) {
            subscription = existingValid.get();

            // PENDING 상태인 경우 경고
            if (subscription.getStatus().isPending()) {
                throw SubscriptionException.paymentPending(
                    "이미 결제 대기 중인 구독이 있습니다."
                );
            }

            expireIfNeeded(subscription);

        } else {
            // 유효한 구독이 없으면 새로 생성
            subscription = startFreeSubscription(user);
        }

        // 플랜 변경
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
     * 구독 취소 - ACTIVE만 취소 가능 (비관적 락 사용)
     */
    @Transactional
    public SubscriptionResponse cancelSubscription(Long userId, String reason) {
        User user = findUserById(userId);

        // ⭐ 비관적 락을 사용하여 ACTIVE 구독만 조회
        Subscription subscription = subscriptionRepository
            .findValidSubscriptionWithLock(user, SubscriptionStatus.UNIQUE_CONSTRAINT_STATUSES)
            .filter(s -> s.getStatus().isActive())  // ACTIVE만
            .orElseThrow(() -> SubscriptionException.subscriptionNotFound(userId));

        subscription.cancel(reason);

        return SubscriptionResponse.from(subscription);
    }

    /**
     * FREE 플랜으로 구독 시작 (내부용)
     */
    private Subscription startFreeSubscription(User user) {
        SubscriptionPlan freePlan = subscriptionPlanService
                .findByNameEntity(DEFAULT_FREE_PLAN_NAME);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = calculateEndDate(freePlan, now);

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(freePlan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .endDate(endDate)
                .autoRenew(true)
                .build();

        return subscriptionRepository.save(subscription);
    }

    /**
     * 구독 만료 확인 및 처리
     */
    private void expireIfNeeded(Subscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE
            && LocalDateTime.now().isAfter(subscription.getEndDate())) {
            log.info("구독 만료 처리: subscriptionId={}", subscription.getId());
            subscription.expire();
        }
    }

    /**
     * 종료일 계산
     */
    private LocalDateTime calculateEndDate(SubscriptionPlan plan, LocalDateTime startDate) {
        if (plan.getBillingPeriod() == null) {
            // 무료 플랜은 100년 (사실상 영구)
            return startDate.plusYears(100);
        }

        return switch (plan.getBillingPeriod()) {
            case MONTHLY -> startDate.plusMonths(1);
            case YEARLY -> startDate.plusYears(1);
        };
    }

    /**
     * 사용자 조회 (내부용)
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "사용자를 찾을 수 없습니다: userId=" + userId
                ));
    }
}