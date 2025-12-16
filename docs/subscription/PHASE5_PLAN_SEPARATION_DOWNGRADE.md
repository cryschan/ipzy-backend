# Phase 5: 플랜 정책 및 사용량 제한 구현

## 🎯 목표
- FREE 플랜 기본 제공 (신규 가입자 자동 부여)
- 옷 추천 결과 일일 사용량 제한 (FREE: 3회, BASIC: 10회)
- BASIC 결제 실패/만료 시 FREE로 다운그레이드
- ⚠️ **간소화된 플랜 구조** (FREE_TRIAL 제거)

---

## 📝 변경사항 요약

### ❌ 제거되는 기능:
- `FREE_TRIAL` (7일 무료 체험) - **완전 제거**
- `hasUsedFreeTrial` 필드 (User 엔티티)
- `freeTrialEndedAt` 필드 (User 엔티티)
- `maxProducts` (상품 개수 제한)
- `maxRecommendations` (추천 횟수 제한)
- `aiStyling` 기능 차이

### ✅ 새로운 기능:
- `dailyQuizLimit` (하루 옷 추천 결과 횟수 제한)
- 한국 시간 기준 일일 사용량 추적 (KST 00:00 ~ 23:59)
- 간소화된 플랜 구조 (FREE → BASIC)

### 플랜 구조:

| 플랜 | 기간 | 가격 | 옷 추천 제한 | 설명 |
|------|------|------|------------|------|
| **FREE** | 영구 | 0원 | 하루 3회 | 기본 무료 플랜 (신규 가입 시 자동 부여) |
| **BASIC** | 1개월 | 2,900원 | 하루 10회 | 유료 플랜 |
| ~~**PRO**~~ | - | - | - | **개발 예정** (백엔드 차단) |

---

## 📝 작업 체크리스트

### 5-1. 플랜 데이터 변경
- [ ] FREE_TRIAL 플랜 제거
- [ ] FREE 플랜 설정 (가격: 0원, dailyQuizLimit: 3)
- [ ] BASIC 플랜 설정 (가격: 2,900원, dailyQuizLimit: 10)
- [ ] PRO 플랜 비활성화 (badge: "COMING_SOON")
- [ ] ⭐ features에서 maxProducts, maxRecommendations, aiStyling 제거

### 5-2. User 엔티티 정리
- [ ] `hasUsedFreeTrial` 필드 제거 (Migration)
- [ ] `freeTrialEndedAt` 필드 제거 (Migration)

### 5-3. 일일 사용량 추적 (Quiz 도메인과 연계)
- [ ] Quiz API에서 사용량 체크 로직 구현
- [ ] 한국 시간 기준 일일 리셋 확인
- [ ] 사용량 초과 시 예외 처리

### 5-4. Service 로직 변경
- [ ] `SubscriptionService.createDefaultSubscription` 단순화 (무조건 FREE)
- [ ] `startFreeTrialSubscription` 메서드 제거
- [ ] 다운그레이드 로직 개선 (BASIC → FREE)

### 5-5. 테스트
- [ ] 신규 가입 시 FREE 플랜 자동 부여 테스트
- [ ] BASIC 만료 시 FREE 다운그레이드 테스트
- [ ] 결제 실패 시 FREE 다운그레이드 테스트

---

## 🔧 구현 코드

### 1. SubscriptionDataInitializer 변경

```java
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

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SubscriptionDataInitializer implements CommandLineRunner {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (subscriptionPlanRepository.count() > 0) {
            log.info("구독 플랜 데이터가 이미 존재합니다.");
            return;
        }

        log.info("구독 플랜 기본 데이터를 초기화합니다...");

        List<SubscriptionPlan> plans = createPlans();
        subscriptionPlanRepository.saveAll(plans);

        log.info("구독 플랜 초기화 완료: 총 {}개", plans.size());
    }

    private List<SubscriptionPlan> createPlans() {
        List<SubscriptionPlan> plans = new ArrayList<>();

        // ==========================================
        // FREE 플랜 (영구 무료, 하루 3회 옷 추천)
        // ==========================================
        Map<String, Object> freeFeatures = new HashMap<>();
        freeFeatures.put("dailyQuizLimit", 3);  // ⭐ 하루 3회 제한

        plans.add(SubscriptionPlan.builder()
                .name("FREE")
                .displayName("무료 플랜")
                .price(0)
                .currency("KRW")
                .billingPeriod(null)  // 영구
                .features(freeFeatures)
                .description("하루 3회 옷 추천 제공 (영구 무료)")
                .badge("FREE")
                .build());

        // ==========================================
        // BASIC 플랜 (2,900원/월, 하루 10회 옷 추천)
        // ==========================================
        Map<String, Object> basicFeatures = new HashMap<>();
        basicFeatures.put("dailyQuizLimit", 10);  // ⭐ 하루 10회 제한

        plans.add(SubscriptionPlan.builder()
                .name("BASIC")
                .displayName("베이직")
                .price(2900)  // ⭐ 2,900원
                .currency("KRW")
                .billingPeriod(BillingPeriod.MONTHLY)
                .features(basicFeatures)
                .description("하루 10회 옷 추천 제공")
                .badge("POPULAR")
                .build());

        // ==========================================
        // PRO 플랜 (개발 예정)
        // ==========================================
        Map<String, Object> proFeatures = new HashMap<>();
        proFeatures.put("dailyQuizLimit", 99999);  // 무제한 (추후 변경 가능)

        plans.add(SubscriptionPlan.builder()
                .name("PRO")
                .displayName("프로")
                .price(9900)
                .currency("KRW")
                .billingPeriod(BillingPeriod.MONTHLY)
                .features(proFeatures)
                .description("개발 예정")
                .badge("COMING_SOON")
                .build());

        return plans;
    }
}
```

---

### 2. User 엔티티 정리 (FREE_TRIAL 필드 제거)

#### Migration SQL

```sql
-- V3__remove_free_trial_fields_from_users.sql

-- FREE_TRIAL 관련 필드 제거
ALTER TABLE users
DROP COLUMN IF EXISTS has_used_free_trial;

ALTER TABLE users
DROP COLUMN IF EXISTS free_trial_ended_at;

-- 기존 데이터가 있을 경우를 대비한 정리
COMMENT ON TABLE users IS '사용자 테이블 (FREE_TRIAL 정책 제거됨)';
```

---

### 3. SubscriptionService 단순화

```java
package com.ipzy.domain.subscription.service;

import com.ipzy._global.common.enums.SubscriptionStatus;
import com.ipzy.domain.subscription.entity.Subscription;
import com.ipzy.domain.subscription.entity.SubscriptionPlan;
import com.ipzy.domain.subscription.repository.SubscriptionRepository;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class SubscriptionService {

    private static final String FREE_PLAN_NAME = "FREE";
    private static final String BASIC_PLAN_NAME = "BASIC";

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanService subscriptionPlanService;
    private final UserRepository userRepository;

    /**
     * ⭐ 기본 구독 생성 (User 생성 시 호출)
     *
     * 모든 신규 가입자는 FREE 플랜 자동 부여
     */
    @Transactional
    public Subscription createDefaultSubscription(Long userId) {
        User user = findUserById(userId);
        return startFreePlanSubscription(user);
    }

    /**
     * FREE (영구) 구독 시작
     */
    private Subscription startFreePlanSubscription(User user) {
        SubscriptionPlan freePlan = subscriptionPlanService
            .findEntityByName(FREE_PLAN_NAME);

        Subscription subscription = Subscription.builder()
            .user(user)
            .plan(freePlan)
            .status(SubscriptionStatus.ACTIVE)
            .startDate(LocalDateTime.now())
            .endDate(null)  // ⭐ 영구 (만료 없음)
            .autoRenew(false)
            .build();

        log.info("FREE 구독 시작: userId={}", user.getId());

        return subscriptionRepository.save(subscription);
    }

    /**
     * ⭐ FREE (영구) 플랜으로 다운그레이드
     *
     * 호출 시점:
     * 1. BASIC 결제 실패 시
     * 2. BASIC 만료 시
     */
    @Transactional
    public Subscription downgradeToFreePlan(User user) {
        SubscriptionPlan freePlan = subscriptionPlanService
            .findEntityByName(FREE_PLAN_NAME);

        // 기존 유효한 구독 조회
        Optional<Subscription> existingValid =
            subscriptionRepository.findValidSubscription(user);

        Subscription subscription;

        if (existingValid.isPresent()) {
            // 기존 구독 업데이트
            subscription = existingValid.get();

            subscription.updatePlan(
                freePlan,
                SubscriptionStatus.ACTIVE,
                LocalDateTime.now(),
                null,  // 영구 (만료 없음)
                false
            );

            log.info("FREE 플랜으로 다운그레이드: userId={}, subscriptionId={}",
                    user.getId(), subscription.getId());

        } else {
            // 유효한 구독이 없으면 새로 생성
            subscription = startFreePlanSubscription(user);
        }

        return subscription;
    }

    /**
     * 만료 처리 (개선)
     */
    private void expireIfNeeded(Subscription subscription) {
        if (subscription == null || subscription.getEndDate() == null) {
            return;  // 영구 플랜은 만료 없음
        }

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE
                && LocalDateTime.now().isAfter(subscription.getEndDate())) {

            subscription.expire();

            // ⭐ BASIC 만료 시 FREE로 다운그레이드
            String planName = subscription.getPlan().getName();
            if (BASIC_PLAN_NAME.equals(planName)) {
                downgradeToFreePlan(subscription.getUser());

                log.info("BASIC 만료 → FREE 다운그레이드: userId={}",
                        subscription.getUser().getId());
            }
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }
}
```

---

### 4. PaymentService 결제 실패 시 다운그레이드

```java
// PaymentService.java

/**
 * 결제 실패 처리 (개선)
 */
@Transactional
public void failPayment(String orderId, String errorCode, String errorMessage) {
    Payment payment = paymentRepository.findById(Long.parseLong(orderId))
        .orElseThrow(() -> PaymentException.notFound(orderId));

    // Payment 실패 처리
    payment.fail(errorCode + ": " + errorMessage);

    // Subscription CANCELLED 처리
    Subscription subscription = payment.getSubscription();
    subscription.cancel("결제 실패: " + errorMessage);

    // ⭐ FREE (영구) 플랜으로 다운그레이드
    subscriptionService.downgradeToFreePlan(subscription.getUser());

    log.warn("결제 실패 → FREE 다운그레이드: userId={}, paymentId={}",
            subscription.getUser().getId(), payment.getId());
}
```

---

### 5. CustomOAuth2UserService 수정 (간소화)

```java
package com.ipzy.domain.auth.service;

// ... imports

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final OAuth2UserInfoFactory oAuth2UserInfoFactory;
    private final SubscriptionService subscriptionService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        // ... OAuth2 사용자 정보 로드 로직

        // ⭐ 사용자 조회 또는 생성
        User user = getOrCreateUser(provider, providerId, email, name, profileImage);

        user.updateLastLoginAt();

        return CustomUserPrincipal.from(user, oauth2User.getAttributes());
    }

    /**
     * 사용자 조회 또는 생성
     */
    private User getOrCreateUser(String provider, String providerId,
                                 String email, String name, String profileImage) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .or(() -> userRepository.findByEmail(email))
                .map(existingUser -> updateExistingUser(existingUser, provider, providerId, name, profileImage))
                .orElseGet(() -> createNewUserWithSubscription(provider, providerId, email, name, profileImage));
    }

    /**
     * ⭐ 신규 사용자 + FREE 구독 생성
     *
     * User 생성 직후 FREE 플랜 구독을 자동으로 생성합니다.
     */
    private User createNewUserWithSubscription(String provider, String providerId,
                                               String email, String name, String profileImage) {
        // 1. User 생성
        User newUser = userRepository.save(User.builder()
                .email(email)
                .name(name)
                .profileImageUrl(profileImage)
                .provider(provider)
                .providerId(providerId)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

        log.info("신규 사용자 생성: userId={}, provider={}, email={}",
                newUser.getId(), provider, email);

        // 2. ⭐ FREE 구독 생성 (무조건 FREE)
        subscriptionService.createDefaultSubscription(newUser.getId());

        log.info("신규 사용자 FREE 구독 생성 완료: userId={}", newUser.getId());

        return newUser;
    }

    // ... 기타 메서드
}
```

---

## 📊 플로우 다이어그램

### 신규 가입 플로우

```
신규 가입
    ↓
FREE 플랜 자동 부여
(하루 3회, 영구 무료)
    ↓
    ┌───────────┐
    ↓           ↓
계속 FREE    BASIC 선택
            (2,900원/월)
                ↓
            결제 성공
                ↓
            BASIC (하루 10회)
```

### 만료/실패 플로우

```
BASIC (ACTIVE) → 1개월 경과 → FREE (영구)

BASIC (PENDING) → 결제 실패 → FREE (영구)
```

---

## 📌 일일 사용량 제한 구현 (Quiz API 연계)

### Quiz API에서 사용량 체크

```java
// QuizService.java (예시)

@Transactional
public QuizResponse getRecommendation(Long userId) {
    User user = findUserById(userId);

    // 1. 사용자의 현재 플랜 조회
    Subscription subscription = subscriptionRepository.findValidSubscription(user)
        .orElseThrow(() -> new IllegalStateException("유효한 구독이 없습니다"));

    // 2. 플랜의 일일 제한 확인
    Integer dailyLimit = (Integer) subscription.getPlan().getFeatures().get("dailyQuizLimit");

    // 3. 오늘 사용량 조회 (한국 시간 기준)
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    int usedToday = quizUsageRepository.countTodayUsage(user, today);

    // 4. 제한 체크
    if (usedToday >= dailyLimit) {
        throw new QuizLimitExceededException(
            String.format("일일 사용량을 초과했습니다. (오늘 사용: %d/%d)", usedToday, dailyLimit)
        );
    }

    // 5. 퀴즈 실행
    QuizResponse response = executeQuiz(user);

    // 6. 사용량 증가
    quizUsageRepository.incrementUsage(user, today);

    return response;
}
```

---

## ✅ 테스트 시나리오

### 1. 신규 가입 시 FREE 자동 부여

```java
@Test
@DisplayName("신규 가입 시 FREE 플랜 자동 부여")
void newUserAutoFree() {
    // Given
    OAuth2UserRequest request = createKakaoUserRequest();

    // When
    OAuth2User result = oAuth2UserService.loadUser(request);

    // Then
    CustomUserPrincipal principal = (CustomUserPrincipal) result;
    Long userId = principal.getUserId();

    // User 생성 확인
    User user = userRepository.findById(userId).orElseThrow();
    assertThat(user.getEmail()).isEqualTo("test@kakao.com");

    // FREE 구독 자동 생성 확인
    Subscription subscription = subscriptionRepository
        .findValidSubscription(user)
        .orElseThrow();

    assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(subscription.getPlan().getName()).isEqualTo("FREE");
    assertThat(subscription.getEndDate()).isNull();  // 영구
    assertThat(subscription.getPlan().getFeatures().get("dailyQuizLimit")).isEqualTo(3);
}
```

### 2. BASIC 만료 시 FREE 다운그레이드

```java
@Test
@DisplayName("BASIC 만료 시 FREE로 다운그레이드")
void basicExpiredDowngrade() {
    // Given: BASIC (ACTIVE)
    User user = createTestUser();
    Subscription basic = createBasicSubscription(user);

    // When: 만료 처리
    basic.setEndDate(LocalDateTime.now().minusDays(1));
    subscriptionService.expireIfNeeded(basic);

    // Then: FREE (영구)로 변경
    Subscription updated = subscriptionRepository.findValidSubscription(user)
        .orElseThrow();

    assertThat(updated.getPlan().getName()).isEqualTo("FREE");
    assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(updated.getEndDate()).isNull();  // 영구
    assertThat(updated.getPlan().getFeatures().get("dailyQuizLimit")).isEqualTo(3);
}
```

### 3. 일일 사용량 제한 테스트

```java
@Test
@DisplayName("FREE 플랜 하루 3회 제한")
void freePlanDailyLimit() {
    // Given: FREE 플랜 사용자
    User user = createTestUserWithFree();
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

    // When: 3회 사용
    for (int i = 0; i < 3; i++) {
        quizService.getRecommendation(user.getId());
    }

    // Then: 4회째 사용 시 예외
    assertThatThrownBy(() -> quizService.getRecommendation(user.getId()))
        .isInstanceOf(QuizLimitExceededException.class)
        .hasMessageContaining("일일 사용량을 초과했습니다");
}
```

---

## 🎯 완료 기준

- [ ] FREE_TRIAL 플랜 및 관련 코드 완전 제거
- [ ] User 엔티티에서 hasUsedFreeTrial, freeTrialEndedAt 필드 제거
- [ ] FREE 플랜 설정 (0원, 하루 3회)
- [ ] BASIC 플랜 설정 (2,900원, 하루 10회)
- [ ] 신규 가입 시 FREE 자동 부여 확인
- [ ] 만료/실패 시 FREE 다운그레이드 확인
- [ ] ⭐ maxProducts, maxRecommendations, aiStyling 제거
- [ ] PRO 플랜 비활성화
- [ ] 테스트 통과

---

## 🚨 주의사항

### 1. PRO 플랜 처리

```java
// SubscriptionController.java

@PostMapping("/subscriptions")
public ApiResponse<SubscriptionResponse> createSubscription(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        @Valid @RequestBody CreateSubscriptionRequest request) {

    SubscriptionPlan plan = subscriptionPlanService.findEntityById(request.planId());

    // PRO 플랜 선택 불가
    if ("PRO".equals(plan.getName())) {
        throw SubscriptionException.planNotAvailable(
            "PRO 플랜은 현재 개발 예정입니다."
        );
    }

    // FREE, BASIC만 허용
    // ...
}
```

### 2. 프론트엔드 대응

```json
// GET /api/subscription/plans 응답
{
  "data": [
    {
      "id": 1,
      "name": "FREE",
      "displayName": "무료 플랜",
      "price": 0,
      "features": {
        "dailyQuizLimit": 3
      },
      "badge": "FREE",
      "available": true
    },
    {
      "id": 2,
      "name": "BASIC",
      "displayName": "베이직",
      "price": 2900,
      "features": {
        "dailyQuizLimit": 10
      },
      "badge": "POPULAR",
      "available": true
    },
    {
      "id": 3,
      "name": "PRO",
      "displayName": "프로",
      "price": 9900,
      "badge": "COMING_SOON",
      "available": false  // ⭐ 선택 불가
    }
  ]
}
```

### 3. 한국 시간 기준 일일 리셋

```java
// 한국 시간 기준으로 날짜 계산
LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

// 사용량 조회 시 항상 한국 시간 기준 사용
```

---

**Estimated Time**: 2-3일
**Priority**: 🟡 Medium
**Dependencies**: Phase 4 완료 후 진행

---

**Last Updated**: 2025-12-16
**Status**: ⭐ 정책 변경 반영 완료