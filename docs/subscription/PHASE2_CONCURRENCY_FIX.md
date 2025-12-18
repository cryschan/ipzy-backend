# Phase 2: 동시성 문제 해결

## 🎯 목표
- `getOrCreateSubscription` Race Condition 해결
- PostgreSQL Partial Unique Constraint 적용
- 공통 쿼리 로직 구현

---

## 📝 작업 체크리스트

- [ ] SubscriptionStatus enum 개선
- [ ] SubscriptionRepository 공통 메서드 추가
- [ ] Migration 스크립트 작성
- [ ] SubscriptionService 중복 방지 로직 추가
- [ ] 기존 중복 데이터 정리
- [ ] 테스트 작성

---

## 🔧 구현 코드

### 1. SubscriptionStatus.java 개선

```java
package com.ipzy._global.common.enums;

import java.util.Set;

public enum SubscriptionStatus {
    ACTIVE,     // 활성 구독
    PENDING,    // 결제 대기
    EXPIRED,    // 만료
    CANCELLED;  // 취소

    /**
     * Unique Constraint 대상 상태들
     * PostgreSQL WHERE 조건: status IN ('ACTIVE', 'PENDING')
     */
    public static final Set<SubscriptionStatus> UNIQUE_CONSTRAINT_STATUSES =
        Set.of(ACTIVE, PENDING);

    /**
     * 이 상태가 Unique Constraint 대상인지 확인
     * (사용자당 하나만 가질 수 있는 상태)
     */
    public boolean isUniquePerUser() {
        return UNIQUE_CONSTRAINT_STATUSES.contains(this);
    }

    /**
     * 활성 상태인지 확인
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * 결제 대기 중인지 확인
     */
    public boolean isPending() {
        return this == PENDING;
    }

    /**
     * 종료된 상태인지 확인 (EXPIRED 또는 CANCELLED)
     */
    public boolean isTerminated() {
        return this == EXPIRED || this == CANCELLED;
    }

    /**
     * 유효한 구독인지 확인 (ACTIVE 또는 PENDING)
     */
    public boolean isValid() {
        return isUniquePerUser();
    }
}
```

---

### 2. SubscriptionRepository.java 공통 메서드 추가

```java
package com.ipzy.domain.subscription.repository;

import com.ipzy.domain.subscription.entity.Subscription;
import com.ipzy.domain.user.entity.User;
import com.ipzy._global.common.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * 사용자의 최신 구독 조회 (모든 상태 포함)
     */
    Optional<Subscription> findTopByUserOrderByCreatedAtDesc(User user);

    /**
     * ⭐ 사용자의 유효한 구독 조회 (ACTIVE 또는 PENDING)
     * Unique Constraint와 동일한 조건
     */
    @Query("SELECT s FROM Subscription s " +
           "WHERE s.user = :user " +
           "AND s.status IN :statuses " +
           "ORDER BY s.createdAt DESC " +
           "LIMIT 1")
    Optional<Subscription> findValidSubscription(
        @Param("user") User user,
        @Param("statuses") Set<SubscriptionStatus> statuses
    );

    /**
     * 사용자의 유효한 구독 조회 (편의 메서드)
     */
    default Optional<Subscription> findValidSubscription(User user) {
        return findValidSubscription(user, SubscriptionStatus.UNIQUE_CONSTRAINT_STATUSES);
    }

    /**
     * 사용자의 모든 구독 히스토리 조회
     */
    List<Subscription> findAllByUserOrderByCreatedAtDesc(User user);

    /**
     * 특정 상태의 구독 개수 조회
     */
    @Query("SELECT COUNT(s) FROM Subscription s " +
           "WHERE s.user = :user " +
           "AND s.status IN :statuses")
    long countByUserAndStatusIn(
        @Param("user") User user,
        @Param("statuses") Set<SubscriptionStatus> statuses
    );

    /**
     * 유효한 구독 개수 조회 (중복 체크용)
     */
    default long countValidSubscriptions(User user) {
        return countByUserAndStatusIn(user, SubscriptionStatus.UNIQUE_CONSTRAINT_STATUSES);
    }
}
```

---

### 3. Migration SQL (Flyway)

#### 3-1. V1__cleanup_duplicate_subscriptions.sql

```sql
-- ==========================================
-- 중복 구독 정리 (Unique Constraint 추가 전)
-- ==========================================

-- 1. 중복 확인
DO $$
DECLARE
    duplicate_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO duplicate_count
    FROM (
        SELECT user_id, status, COUNT(*) as cnt
        FROM subscriptions
        WHERE status IN ('ACTIVE', 'PENDING')
        GROUP BY user_id, status
        HAVING COUNT(*) > 1
    ) duplicates;

    RAISE NOTICE '중복된 구독 발견: % 건', duplicate_count;
END $$;

-- 2. 중복 제거 (최신 것만 유지, 나머지는 CANCELLED로)
UPDATE subscriptions s1
SET
    status = 'CANCELLED',
    cancel_reason = 'System: Duplicate cleanup before unique constraint',
    cancelled_at = NOW()
WHERE s1.status IN ('ACTIVE', 'PENDING')
  AND EXISTS (
    SELECT 1 FROM subscriptions s2
    WHERE s2.user_id = s1.user_id
      AND s2.status IN ('ACTIVE', 'PENDING')
      AND s2.created_at > s1.created_at
  );

-- 3. 정리 결과 확인
SELECT
    user_id,
    status,
    COUNT(*) as count
FROM subscriptions
WHERE status IN ('ACTIVE', 'PENDING')
GROUP BY user_id, status
HAVING COUNT(*) > 1;
```

#### 3-2. V2__create_unique_constraint.sql

```sql
-- ==========================================
-- Partial Unique Index 생성
-- ==========================================

-- ACTIVE 또는 PENDING 상태는 사용자당 하나만 허용
CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_user_valid_status
ON subscriptions (user_id)
WHERE status IN ('ACTIVE', 'PENDING');

-- 인덱스 설명 추가
COMMENT ON INDEX uk_subscription_user_valid_status IS
'사용자당 하나의 유효한 구독(ACTIVE 또는 PENDING)만 허용. EXPIRED/CANCELLED는 히스토리로 여러 개 가능.';

-- 검증
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE indexname = 'uk_subscription_user_valid_status';
```

---

### 4. SubscriptionService.java 개선

```java
package com.ipzy.domain.subscription.service;

import org.springframework.dao.DataIntegrityViolationException;
// ... 기타 import

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class SubscriptionService {

    private static final String DEFAULT_FREE_PLAN_NAME = "FREE";
    private static final int FREE_TRIAL_DAYS = 7;

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanService subscriptionPlanService;
    private final UserRepository userRepository;

    // ... 기존 메서드들

    /**
     * 사용자의 구독을 조회하거나 생성 (상태 무관)
     *
     * 동시성 문제 해결:
     * 1. Unique Constraint로 DB 레벨 보장
     * 2. 예외 발생 시 재조회
     */
    @Transactional
    public Subscription ensureDefaultSubscription(Long userId) {
        User user = findUserById(userId);

        try {
            return getOrCreateSubscription(user);

        } catch (DataIntegrityViolationException e) {
            // Unique constraint 위반 시 재조회
            log.warn("동시 구독 생성 감지, 재조회: userId={}", userId);

            return subscriptionRepository.findValidSubscription(user)
                .orElseThrow(() -> new IllegalStateException(
                    "구독 생성 실패: userId=" + userId
                ));
        }
    }

    private Subscription getOrCreateSubscription(User user) {
        // ⭐ 유효한 구독 조회 (ACTIVE 또는 PENDING)
        Optional<Subscription> validSubscription =
            subscriptionRepository.findValidSubscription(user);

        if (validSubscription.isPresent()) {
            Subscription subscription = validSubscription.get();
            expireIfNeeded(subscription);
            return subscription;
        }

        // 없으면 FREE 플랜 생성
        return startFreeSubscription(user);
    }

    /**
     * 구독 생성/변경
     */
    @Transactional
    public SubscriptionResponse createSubscription(Long userId,
                                                   CreateSubscriptionRequest request) {
        User user = findUserById(userId);

        // ⭐ 유효한 구독 조회 (ACTIVE 또는 PENDING)
        Optional<Subscription> existingValid =
            subscriptionRepository.findValidSubscription(user);

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
     * 구독 취소 - ACTIVE만 취소 가능
     */
    @Transactional
    public SubscriptionResponse cancelSubscription(Long userId, String reason) {
        User user = findUserById(userId);

        // ⭐ ACTIVE 구독만 조회
        Subscription subscription = subscriptionRepository
            .findValidSubscription(user)
            .filter(s -> s.getStatus().isActive())  // ACTIVE만
            .orElseThrow(() -> SubscriptionException.subscriptionNotFound(userId));

        subscription.cancel(reason);

        return SubscriptionResponse.from(subscription);
    }

    // ... 기타 메서드
}
```

---

## ✅ 테스트 시나리오

### 1. 동시성 테스트

```java
@SpringBootTest
class SubscriptionServiceConcurrencyTest {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("동시 구독 생성 시 하나만 생성되어야 함")
    void concurrentSubscriptionCreation() throws Exception {
        // Given
        User user = createTestUser();

        // When: 10개 스레드가 동시에 구독 생성 시도
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        List<Future<Subscription>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            futures.add(executor.submit(() -> {
                latch.countDown();
                latch.await();  // 모두 대기 후 동시 실행
                return subscriptionService.ensureDefaultSubscription(user.getId());
            }));
        }

        // Then: 모든 스레드가 같은 구독을 반환받아야 함
        Set<Long> subscriptionIds = futures.stream()
            .map(f -> {
                try {
                    return f.get().getId();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toSet());

        assertThat(subscriptionIds).hasSize(1);  // 하나의 구독만 생성됨

        // DB 확인
        long count = subscriptionRepository.countValidSubscriptions(user);
        assertThat(count).isEqualTo(1);

        executor.shutdown();
    }
}
```

### 2. Unique Constraint 테스트

```java
@Test
@DisplayName("ACTIVE/PENDING 중복 생성 시 예외 발생")
void duplicateActiveSubscription() {
    // Given
    User user = createTestUser();
    Subscription subscription1 = createActiveSubscription(user);

    // When & Then
    assertThatThrownBy(() -> {
        Subscription subscription2 = Subscription.builder()
            .user(user)
            .plan(freePlan)
            .status(SubscriptionStatus.ACTIVE)
            .startDate(LocalDateTime.now())
            .endDate(LocalDateTime.now().plusDays(7))
            .build();
        subscriptionRepository.save(subscription2);
        subscriptionRepository.flush();  // 강제로 DB 반영
    })
    .isInstanceOf(DataIntegrityViolationException.class)
    .hasMessageContaining("uk_subscription_user_valid_status");
}
```

---

## 🎯 완료 기준

- [ ] PostgreSQL Unique Index 생성 확인
- [ ] 동시성 테스트 통과
- [ ] 중복 구독 방지 확인
- [ ] 기존 기능 정상 작동
- [ ] 로그에 "동시 구독 생성 감지" 메시지 없음 (운영 환경)

---

**Estimated Time**: 2-3시간
**Priority**: 🔥 Critical (운영 환경 배포 전 필수)