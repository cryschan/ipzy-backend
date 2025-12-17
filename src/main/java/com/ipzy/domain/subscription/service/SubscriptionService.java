package com.ipzy.domain.subscription.service;

import com.ipzy._global.common.enums.BillingPeriod;
import com.ipzy._global.common.enums.SubscriptionStatus;
import com.ipzy.domain.subscription.dto.Request.CreateSubscriptionRequest;
import com.ipzy.domain.subscription.dto.Response.SubscriptionPlanResponse;
import com.ipzy.domain.subscription.dto.Response.SubscriptionResponse;
import com.ipzy.domain.subscription.entity.Subscription;
import com.ipzy.domain.subscription.entity.SubscriptionPlan;
import com.ipzy.domain.subscription.exception.SubscriptionException;
import com.ipzy.domain.subscription.repository.SubscriptionRepository;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.repository.UserRepository;
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
     * ⭐ 내 구독 조회 (순수 조회만)
     * - 읽기 작업에서 데이터 변경하지 않음
     * - 구독이 없으면 예외 발생
     * - 만료 처리하지 않음 (읽기 전용)
     *
     * @param userId 사용자 ID
     * @return 구독 정보 (ACTIVE, PENDING, EXPIRED 등 모든 상태)
     * @throws SubscriptionException 구독이 없을 경우
     */
    public SubscriptionResponse getMySubscription(Long userId) {
        User user = findUserById(userId);

        Subscription subscription = subscriptionRepository
                .findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> SubscriptionException.subscriptionNotFound(userId));

        return SubscriptionResponse.from(subscription);
    }

    /**
     * 사용자의 구독을 조회하거나 생성 (UserService 전용)
     * - 회원가입 시에만 호출
     * - 비관적 락으로 동시성 제어
     * - 구독이 없으면 FREE 플랜 생성
     * - 만료된 구독 자동 처리
     *
     * @param userId 사용자 ID
     * @return 구독 엔티티
     */
    @Transactional
    public Subscription ensureDefaultSubscription(Long userId) {
        User user = findUserById(userId);

        Optional<Subscription> latest = subscriptionRepository
                .findTopByUserOrderByCreatedAtDesc(user);

        if (latest.isPresent()) {
            Subscription subscription = latest.get();
            expireIfNeeded(subscription);
            return subscription;
        }

        // 구독이 없으면 FREE 플랜 생성
        log.info("사용자의 구독이 없어 FREE 플랜 생성: userId={}", userId);
        return startFreeSubscription(user);
    }

    /**
     * 구독 생성/변경 (비관적 락 사용)
     */
    @Transactional
    public SubscriptionResponse createSubscription(Long userId, CreateSubscriptionRequest request) {
        // 기존 구독 조회 또는 생성
        Subscription subscription = ensureDefaultSubscription(userId);

        // PENDING 상태인 경우 경고
        if (subscription.getStatus().isPending()) {
            throw SubscriptionException.paymentPending(
                "이미 결제 대기 중인 구독이 있습니다."
            );
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

        Subscription subscription = subscriptionRepository
            .findTopByUserOrderByCreatedAtDesc(user)
            .filter(s -> s.getStatus().isActive())
            .orElseThrow(() -> SubscriptionException.subscriptionNotFound(userId));

        subscription.cancel(reason);

        return SubscriptionResponse.from(subscription);
    }

    /**
     * FREE 플랜으로 구독 시작 (내부용)
     * FREE 플랜은 endDate = null (영구)
     */
    private Subscription startFreeSubscription(User user) {
        SubscriptionPlan freePlan = subscriptionPlanService
                .findEntityByName(DEFAULT_FREE_PLAN_NAME);

        LocalDateTime now = LocalDateTime.now();

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(freePlan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .endDate(null)  // ⭐ FREE 플랜은 영구 (endDate = null)
                .autoRenew(false)  // FREE는 자동 갱신 불필요
                .build();

        return subscriptionRepository.save(subscription);
    }

    /**
     * 구독 만료 확인 및 FREE로 다운그레이드
     * 유료 플랜 만료 시 → FREE 플랜으로 자동 다운그레이드
     * FREE 플랜은 endDate = null이므로 만료 안 됨
     */
    private void expireIfNeeded(Subscription subscription) {
        // endDate가 null이면 FREE 플랜 (만료 없음)
        if (subscription.getEndDate() == null) {
            return;
        }

        // ACTIVE 상태이고 endDate가 지났으면 다운그레이드
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE
            && LocalDateTime.now().isAfter(subscription.getEndDate())) {

            log.info("유료 플랜 만료 → FREE로 다운그레이드: subscriptionId={}",
                     subscription.getId());

            // ⭐ FREE 플랜으로 다운그레이드
            SubscriptionPlan freePlan = subscriptionPlanService
                    .findEntityByName(DEFAULT_FREE_PLAN_NAME);

            LocalDateTime now = LocalDateTime.now();
            subscription.updatePlan(
                    freePlan,
                    SubscriptionStatus.ACTIVE,  // status는 ACTIVE 유지
                    now,
                    null,  // endDate = null (영구)
                    false
            );
        }
    }

    /**
     * 종료일 계산
     * FREE 플랜: null (영구)
     * BASIC/PRO: 결제 주기에 따라 계산
     */
    private LocalDateTime calculateEndDate(SubscriptionPlan plan, LocalDateTime startDate) {
        // ⭐ FREE 플랜은 endDate = null (영구)
        if (plan.getPrice() == 0) {
            return null;
        }

        if (plan.getBillingPeriod() == null) {
            return startDate.plusMonths(1);  // 기본 1개월
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
