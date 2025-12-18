# 구독 시스템 구현 로드맵

## 📌 개요

변경된 요구사항을 반영한 구독 시스템 구현 계획입니다.

### ❌ 제거된 기능:
- **FREE_TRIAL (7일 무료 체험)** - 완전 제거
- 상품 개수 제한 (maxProducts)
- 추천 횟수 제한 (maxRecommendations)
- AI 스타일링 기능 차이 (aiStyling)
- PRO 플랜 개발 (프론트: "개발 예정" 표시)

### ✅ 핵심 기능:
- **FREE (영구 무료, 하루 3회 옷 추천)**
- **BASIC (2,900원/월, 하루 10회 옷 추천)**
- 일일 사용량 제한 (dailyQuizLimit)
- 한국 시간 기준 일일 리셋 (KST 00:00)

---

## 🎯 Phase 별 실행 계획

### **Phase 1: 현재 상태** ✅ (완료)

**완료된 기능:**
- FREE 플랜 자동 부여
- BASIC 선택 시 PENDING
- 구독 조회/생성/취소 API
- Swagger 문서화

**상태:** MVP 기능 완료

---

### **Phase 2: 동시성 문제 해결** 🔥

**우선순위:** Critical (최우선)
**소요 시간:** 2-3시간
**의존성:** 없음 (바로 시작 가능)

**목표:**
- Race Condition 해결
- PostgreSQL Partial Unique Constraint
- 공통 쿼리 로직

**주요 작업:**
1. SubscriptionStatus enum 개선
2. SubscriptionRepository 공통 메서드
3. Migration (중복 정리 + Unique Index)
4. SubscriptionService 중복 방지 로직

**상세 문서:** [PHASE2_CONCURRENCY_FIX.md](./PHASE2_CONCURRENCY_FIX.md)

---

### **Phase 3: User 생성 개선** 🟡

**우선순위:** Medium
**소요 시간:** 1-2시간
**의존성:** Phase 2 완료 권장

**목표:**
- User와 Subscription 생성 로직 개선
- 코드 가독성 향상
- TODO 주석 해결

**주요 작업:**
1. CustomOAuth2UserService 리팩토링
2. 메서드 분리 (3개)
3. SubscriptionService.createDefaultSubscription 단순화 (무조건 FREE 부여)

**상세 문서:** [PHASE3_USER_CREATION_IMPROVEMENT.md](./PHASE3_USER_CREATION_IMPROVEMENT.md)

---

### **Phase 4: 결제 연동** 🔥

**우선순위:** High (수익화 핵심)
**소요 시간:** 1주일
**의존성:** Phase 2, 3 완료 후

**목표:**
- 토스페이먼츠 연동 (BASIC만, 2,900원)
- PENDING → ACTIVE 전환
- 결제 실패 시 FREE 다운그레이드

**주요 작업:**
1. PaymentRepository, PaymentService 구현
2. TossPaymentsClient 구현
3. PaymentController 구현
4. 결제 요청/승인/실패 API
5. PRO 플랜 선택 차단

**상세 문서:** [PHASE4_PAYMENT_INTEGRATION.md](./PHASE4_PAYMENT_INTEGRATION.md)

---

### **Phase 5: 플랜 정책 및 사용량 제한** 🟡

**우선순위:** Medium
**소요 시간:** 2-3일
**의존성:** Phase 4 완료 후

**목표:**
- FREE 플랜 기본 제공 (신규 가입자 자동 부여)
- 옷 추천 결과 일일 사용량 제한 구현
- BASIC 만료/실패 시 FREE 다운그레이드
- ⚠️ **FREE_TRIAL 관련 코드 제거**

**주요 작업:**
1. FREE_TRIAL 플랜 및 관련 코드 완전 제거
2. User 엔티티에서 hasUsedFreeTrial 필드 제거
3. FREE 플랜 설정 (0원, dailyQuizLimit: 3)
4. BASIC 플랜 설정 (2,900원, dailyQuizLimit: 10)
5. ⭐ maxProducts, maxRecommendations, aiStyling 제거
6. PRO 플랜 비활성화
7. 일일 사용량 추적 로직 (Quiz API 연계)

**상세 문서:** [PHASE5_PLAN_SEPARATION_DOWNGRADE.md](./PHASE5_PLAN_SEPARATION_DOWNGRADE.md)

---

### **Phase 6: 스케줄러 & 자동화** 🟢

**우선순위:** Low
**소요 시간:** 2-3일
**의존성:** Phase 5 완료 후

**목표:**
- 만료 자동 처리
- 다운그레이드 자동화
- 알림 발송

**주요 작업:**
1. SubscriptionScheduler 구현
2. NotificationService 구현
3. 이메일 설정
4. 모니터링 로그

**상세 문서:** [PHASE6_SCHEDULER_AUTOMATION.md](./PHASE6_SCHEDULER_AUTOMATION.md)

---

## 📊 전체 타임라인

```
Week 1:
├─ Day 1: Phase 2 (동시성 문제) ✅
├─ Day 2: Phase 3 (User 생성 개선) ✅
└─ Day 3-7: Phase 4 (결제 연동) 🔥

Week 2:
├─ Day 1-3: Phase 5 (플랜 정책 및 사용량 제한)
└─ Day 4-6: Phase 6 (스케줄러 & 자동화)

Total: 약 2주
```

---

## 🎯 우선순위별 분류

### 🔥 Critical (즉시 시작)
1. **Phase 2** - 동시성 문제 (운영 배포 전 필수)

### 🔥 High (수익화 필수)
2. **Phase 4** - 결제 연동 (BASIC 플랜, 2,900원)

### 🟡 Medium (UX 개선)
3. **Phase 3** - User 생성 개선
4. **Phase 5** - 플랜 정책 및 사용량 제한

### 🟢 Low (자동화/최적화)
5. **Phase 6** - 스케줄러 & 자동화

---

## 📁 파일 구조 (최종)

```
com.ipzy.domain.subscription/
├── controller/
│   └── SubscriptionController.java      ✅
├── service/
│   ├── SubscriptionService.java         ✅ (Phase 2, 5에서 개선)
│   └── SubscriptionPlanService.java     ✅
├── repository/
│   ├── SubscriptionRepository.java      ✅ (Phase 2에서 개선)
│   └── SubscriptionPlanRepository.java  ✅
├── entity/
│   ├── Subscription.java                ✅
│   └── SubscriptionPlan.java            ✅
├── dto/
│   ├── Request/
│   │   └── CreateSubscriptionRequest.java  ✅
│   └── Response/
│       ├── SubscriptionPlanResponse.java   ✅
│       └── SubscriptionResponse.java       ✅
├── exception/
│   ├── SubscriptionErrorCode.java       ✅
│   └── SubscriptionException.java       ✅
├── config/
│   └── SubscriptionDataInitializer.java ✅ (Phase 5에서 수정)
└── scheduler/
    └── SubscriptionScheduler.java       🆕 Phase 6

com.ipzy.domain.payment/
├── controller/
│   └── PaymentController.java           🆕 Phase 4
├── service/
│   ├── PaymentService.java              🆕 Phase 4
│   └── TossPaymentsClient.java          🆕 Phase 4
├── repository/
│   └── PaymentRepository.java           🆕 Phase 4
└── entity/
    └── Payment.java                     ✅

com.ipzy.domain.auth/
└── service/
    └── CustomOAuth2UserService.java     ✅ (Phase 3에서 개선)

com.ipzy.domain.user/
└── entity/
    └── User.java                        ✅ (Phase 5에서 필드 제거)

com.ipzy.domain.notification/
└── service/
    └── NotificationService.java         🆕 Phase 6

com.ipzy.domain.quiz/
├── service/
│   └── QuizService.java                 ✅ (일일 사용량 체크 추가)
└── repository/
    └── QuizUsageRepository.java         🆕 Phase 5 (사용량 추적)
```

---

## 📝 Migration Scripts

```
src/main/resources/db/migration/
├── V1__cleanup_duplicate_subscriptions.sql    (Phase 2)
├── V2__create_unique_constraint.sql           (Phase 2)
└── V3__remove_free_trial_fields_from_users.sql (Phase 5)
```

---

## ✅ 체크리스트

### Phase 2: 동시성
- [ ] SubscriptionStatus enum 개선
- [ ] Repository 공통 메서드
- [ ] Migration 실행
- [ ] 동시성 테스트

### Phase 3: User 생성
- [ ] CustomOAuth2UserService 리팩토링
- [ ] 메서드 3개 분리
- [ ] createDefaultSubscription 단순화 (무조건 FREE)

### Phase 4: 결제
- [ ] 토스페이먼츠 계정
- [ ] PaymentService 구현
- [ ] 결제 API 3개
- [ ] PRO 플랜 차단

### Phase 5: 플랜 정책
- [ ] FREE_TRIAL 관련 코드 완전 제거
- [ ] User 필드 제거 (hasUsedFreeTrial, freeTrialEndedAt)
- [ ] FREE 플랜 설정 (0원, 하루 3회)
- [ ] BASIC 플랜 설정 (2,900원, 하루 10회)
- [ ] 다운그레이드 로직
- [ ] ⭐ maxProducts, maxRecommendations, aiStyling 제거

### Phase 6: 스케줄러
- [ ] SubscriptionScheduler 구현
- [ ] NotificationService 구현
- [ ] 이메일 설정
- [ ] 모니터링

---

## 🚨 주의사항

### 1. PRO 플랜 처리

**백엔드:**
```java
// PRO 플랜 선택 시 예외
if ("PRO".equals(plan.getName())) {
    throw SubscriptionException.planNotAvailable(
        "PRO 플랜은 현재 개발 예정입니다."
    );
}
```

**프론트엔드:**
```jsx
// PRO 플랜 UI
<PlanCard
  name="PRO"
  badge="개발 예정"
  disabled={true}  // 선택 불가
/>
```

### 2. 플랜 기능 제한 변경

**변경 전:**
```java
features.put("maxProducts", 10);
features.put("maxRecommendations", 5);
features.put("aiStyling", true);

// 상품 생성 시 체크
if (currentCount >= maxProducts) {
    throw new LimitExceededException();
}
```

**변경 후:**
```java
// ⭐ dailyQuizLimit만 사용
features.put("dailyQuizLimit", 3);   // FREE
features.put("dailyQuizLimit", 10);  // BASIC

// Quiz API에서 일일 사용량 체크
LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
int usedToday = quizUsageRepository.countTodayUsage(user, today);
if (usedToday >= dailyLimit) {
    throw new QuizLimitExceededException();
}
```

### 3. 기존 TODO 주석 정리

**제거할 TODO:**
```java
// TODO (Phase 2): FREE_TRIAL과 FREE 플랜 분리
// → FREE_TRIAL 완전 제거됨, 제거 필요

// TODO (Phase 2): 결제 구현 후 추가
// → Phase 4, 5에서 구현 후 제거
```

---

## 📚 참고 문서

- [구독 플로우](./SUBSCRIPTION_FLOW.md)
- [기능 현황](./SUBSCRIPTION_PAYMENT_STATUS.md)
- [Phase 5: 플랜 정책 및 사용량 제한](./PHASE5_PLAN_SEPARATION_DOWNGRADE.md)

---

## 🎉 완료 후 최종 구조

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
                ↓
            만료/실패 시
                ↓
            FREE (하루 3회)
```

**플랜 특징:**
- ✅ 옷 추천 결과 일일 횟수 제한 (FREE: 3회, BASIC: 10회)
- ✅ 한국 시간 기준 일일 리셋 (KST 00:00)
- ✅ 상품 개수 제한 없음
- ✅ FREE_TRIAL 없음 (간소화)
- ✅ PRO 플랜 개발 예정

---

**Last Updated**: 2025-12-16
**Total Estimated Time**: 2주
**Current Phase**: Phase 1 완료 → Phase 2 시작 권장
**Status**: ⭐ 정책 변경 반영 완료