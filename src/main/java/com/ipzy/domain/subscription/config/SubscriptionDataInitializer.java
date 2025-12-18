package com.ipzy.domain.subscription.config;

import com.ipzy._global.common.enums.BillingPeriod;
import com.ipzy.domain.subscription.entity.SubscriptionPlan;
import com.ipzy.domain.subscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 구독 초기 데이터 생성
 * Free / Basic / Pro
 */

@Slf4j
@Component
@Order(1) //로그인 후 바로 Free plan
@RequiredArgsConstructor
public class SubscriptionDataInitializer implements CommandLineRunner {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // 이미 데이터가 있으면 초기화하지 않음
        if (subscriptionPlanRepository.count() > 0) {
            log.info("구독 플랜 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("구독 플랜 기본 데이터를 초기화합니다...");

        List<SubscriptionPlan> plans = createPlans();
        subscriptionPlanRepository.saveAll(plans);

        log.info("구독 플랜 초기화 완료: 총 {}개 (FREE, BASIC, PRO)", plans.size());
    }

    private List<SubscriptionPlan> createPlans() {
        List<SubscriptionPlan> plans = new ArrayList<>();

        // ==========================================
        // FREE 플랜 (7일 무료 체험)
        //
        // 현재 (Phase 1):
        // - 신규 가입 시 자동 부여
        // - 7일 후 EXPIRED 상태로 전환
        // - BASIC/PRO로 업그레이드 가능
        //
        // TODO (Phase 2): FREE_TRIAL과 FREE 플랜 분리 필요
        // - FREE_TRIAL: 7일 체험 (현재 FREE와 동일, 1회 한정)
        //   - maxProducts: 10개
        //   - maxRecommendations: 5개
        // - FREE: 영구 무료 (결제 실패/만료 시 다운그레이드 대상)
        //   - maxProducts: 5개 (더 제한적)
        //   - maxRecommendations: 3개 (더 제한적)
        // ==========================================
        Map<String, Object> freeFeatures = new HashMap<>();
        freeFeatures.put("maxProducts", 10);
        freeFeatures.put("maxRecommendations", 5);
        freeFeatures.put("aiStyling", false);
        freeFeatures.put("prioritySupport", false);

        plans.add(SubscriptionPlan.builder()
                .name("FREE")
                .displayName("무료 체험")
                .price(0)
                .currency("KRW")
                .billingPeriod(null) // 무료 플랜은 billing period 없음
                .features(freeFeatures)
                .description("7일 무료 체험 플랜")
                .badge("FREE")
                .build());

        // ==========================================
        // BASIC 플랜
        // ==========================================
        Map<String, Object> basicFeatures = new HashMap<>();
        basicFeatures.put("maxProducts", 50);
        basicFeatures.put("maxRecommendations", 20);
        basicFeatures.put("aiStyling", true);
        basicFeatures.put("prioritySupport", false);

        plans.add(SubscriptionPlan.builder()
                .name("BASIC")
                .displayName("베이직")
                .price(9900)
                .currency("KRW")
                .billingPeriod(BillingPeriod.MONTHLY)
                .features(basicFeatures)
                .description("개인 사용자를 위한 기본 플랜")
                .badge("POPULAR")
                .build());

        // ==========================================
        // PRO 플랜
        // ==========================================
        Map<String, Object> proFeatures = new HashMap<>();
        proFeatures.put("maxProducts", -1); // unlimited
        proFeatures.put("maxRecommendations", -1); // unlimited
        proFeatures.put("aiStyling", true);
        proFeatures.put("prioritySupport", true);

        plans.add(SubscriptionPlan.builder()
                .name("PRO")
                .displayName("프로")
                .price(19900)
                .currency("KRW")
                .billingPeriod(BillingPeriod.MONTHLY)
                .features(proFeatures)
                .description("프로페셔널을 위한 프리미엄 플랜")
                .badge("BEST")
                .build());

        return plans;
    }

}

