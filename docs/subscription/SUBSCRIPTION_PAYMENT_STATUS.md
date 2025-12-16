# 구독(Subscription) & 결제(Payment) 기능 현황

## 📊 프로젝트 개요

사용자 구독 관리 및 결제 시스템 구축
- **플랜**: Free, Basic, Pro
- **결제 연동**: 토스페이먼츠
- **상태**: MVP 개발 중

---

## ✅ 완료된 기능

### 1. 엔티티 설계
- [x] **Subscription** 엔티티
  - User, SubscriptionPlan 연관관계
  - 상태: PENDING, ACTIVE, CANCELLED, EXPIRED
  - 메서드: `activate()`, `expire()`, `cancel(String reason)`, `renew(LocalDateTime newEndDate)`, `updatePlan()`, `isActive()`, `isExpired()`
  - 취소 관련 필드: `cancelledAt`, `cancelReason`

- [x] **SubscriptionPlan** 엔티티
  - 플랜 정보: name, displayName, price, currency, billingPeriod
  - features: JSONB 타입으로 플랜별 기능 저장
  - 추가 필드: `description`, `badge`

- [x] **Payment** 엔티티
  - User, Subscription 연관관계
  - 상태: PENDING, COMPLETED, FAILED, REFUNDED, CANCELLED
  - 메서드: `complete()`, `fail()`, `refund()`, `cancel()`, `isCompleted()`
  - 결제 추적: `transactionId`, `receiptUrl`, `failureReason`, `completedAt`

### 2. Repository Layer
- [x] **SubscriptionPlanRepository**
  - `findByName(String name)`: 플랜 이름으로 조회

- [x] **SubscriptionRepository**
  - `findTopByUserOrderByCreatedAtDesc(User)`: 사용자의 최신 구독 조회

- [ ] **PaymentRepository** (미구현)

### 3. Service Layer
- [x] **SubscriptionPlanService**
  - `findAll()`: 전체 플랜 목록 조회
  - `findByName(String name)`: 플랜 이름으로 조회
  - `findEntityById(Long planId)`: 내부용 Entity 조회
  - `findEntityByName(String name)`: 내부용 Entity 조회

- [x] **SubscriptionService**
  - `findAllPlans()`: 플랜 목록 위임
  - `findPlanByName(String name)`: 특정 플랜 조회
  - `getMySubscription(Long userId)`: 내 구독 조회 (구독 없으면 FREE 플랜 자동 생성)
  - `createSubscription(Long userId, CreateSubscriptionRequest)`: 구독 생성 (FREE: 즉시 ACTIVE, 유료: PENDING)
  - `cancelSubscription(Long userId, String reason)`: 구독 취소
  - `ensureDefaultSubscription(Long userId)`: 기본 구독 보장 (없으면 FREE 플랜 생성)
  - `expireIfNeeded(Subscription)`: 만료된 구독 자동 처리
  - `calculateEndDate(SubscriptionPlan, LocalDateTime)`: 플랜별 종료일 계산

### 4. DTO 구조
- [x] **Request/Response 폴더 분리**
  - `SubscriptionPlanResponse`: 플랜 정보 응답
  - `SubscriptionResponse`: 구독 정보 응답
  - `CreateSubscriptionRequest`: 구독 생성 요청

### 5. 예외 처리
- [x] **SubscriptionErrorCode** (001~009)
  - PLAN_NOT_FOUND, NO_PLANS_AVAILABLE, SUBSCRIPTION_NOT_FOUND 등

- [x] **SubscriptionException**
  - 팩토리 메서드 패턴: `planNotFound()`, `subscriptionNotFound()` 등

### 6. API 구현
- [x] **GET /api/subscription/plans**
  - 전체 플랜 목록 조회
  - Swagger 문서화 완료
  - 예외 처리 완료 (플랜 없을 시 404)
  - 인증 필요

- [x] **GET /api/subscription/me**
  - 내 구독 조회
  - 구독 없으면 FREE 플랜 자동 생성
  - 플랜 정보 포함 (중첩 DTO)
  - 자동 만료 처리
  - Swagger 문서화 완료

- [x] **POST /api/subscription/subscriptions**
  - 구독 생성/변경 (플랜 변경 포함)
  - FREE 플랜: 즉시 ACTIVE
  - 유료 플랜: PENDING (결제 대기)
  - 사용자 인증 연동
  - Swagger 문서화 완료

- [x] **DELETE /api/subscription/cancel**
  - 구독 취소
  - 취소 사유 입력 (선택)
  - ACTIVE 구독만 취소 가능
  - Swagger 문서화 완료

---

## 🚧 추후 구현 예정 기능

### Phase 1: 구독 기본 기능 ✅ 완료

#### 1. 구독 생성 API
- [x] **POST /api/subscription/subscriptions**
  - Free 플랜: 즉시 ACTIVE 처리 ✅
  - 유료 플랜: PENDING 상태 → 결제 연동 필요 ✅
  - 플랜 변경 기능 (`updatePlan` 메서드) ✅
  - 사용자 인증 연동 (`@AuthenticationPrincipal`) ✅

#### 2. 내 구독 조회 API
- [x] **GET /api/subscription/me**
  - 현재 로그인한 사용자의 구독 조회 ✅
  - 플랜 정보 포함 (중첩 DTO) ✅
  - 구독 없으면 FREE 플랜 자동 생성 ✅
  - 자동 만료 처리 (`expireIfNeeded`) ✅

#### 3. 구독 취소 API
- [x] **DELETE /api/subscription/cancel**
  - 활성 구독 취소 처리 ✅
  - 취소 사유 입력 (선택) ✅
  - `cancelledAt`, `cancelReason` 필드 저장 ✅
  - `autoRenew` 자동 비활성화 ✅

### Phase 2: 결제 연동 (토스페이먼츠) - 다음 우선순위

#### 1. 토스페이먼츠 연동
- [ ] **결제 요청 API**
  - `POST /api/payment/toss/request`
  - Basic/Pro 플랜 구독 시 결제 URL 생성

- [ ] **결제 승인 API**
  - `POST /api/payment/toss/confirm`
  - 결제 성공 시 Subscription ACTIVE 처리
  - Payment 엔티티 생성 및 저장

- [ ] **결제 실패 처리**
  - `POST /api/payment/toss/fail`
  - Subscription CANCELLED 처리
  - 실패 사유 저장

#### 2. 웹훅 처리
- [ ] **결제 상태 변경 웹훅**
  - 토스페이먼츠 웹훅 수신
  - Payment 및 Subscription 상태 자동 업데이트

#### 3. PaymentService 구현
- [ ] `createPayment()`: 결제 생성
- [ ] `completePayment()`: 결제 완료 처리
- [ ] `failPayment()`: 결제 실패 처리
- [ ] `refundPayment()`: 환불 처리

### Phase 3: 구독 관리 자동화

#### 1. 구독 만료 처리
- [ ] **스케줄러 구현**
  - 매일 자동으로 만료된 구독 확인
  - `endDate`가 지난 ACTIVE 구독 → EXPIRED 처리

#### 2. 자동 갱신 처리
- [ ] **구독 갱신 스케줄러**
  - `autoRenew = true`인 구독 자동 갱신
  - 결제 자동 처리
  - 실패 시 알림

#### 3. 알림 기능
- [ ] 구독 만료 예정 알림 (7일 전, 1일 전)
- [ ] 결제 실패 알림
- [ ] 구독 갱신 완료 알림

### Phase 4: 관리자 기능

#### 1. 플랜 관리 API
- [ ] **POST /api/admin/subscription-plans**: 플랜 생성
- [ ] **PUT /api/admin/subscription-plans/{id}**: 플랜 수정
- [ ] **DELETE /api/admin/subscription-plans/{id}**: 플랜 삭제

#### 2. 구독 관리 API
- [ ] **GET /api/admin/subscriptions**: 전체 구독 목록 조회 (페이징)
- [ ] **GET /api/admin/subscriptions/{id}**: 특정 구독 상세 조회
- [ ] **POST /api/admin/subscriptions/{id}/refund**: 강제 환불

#### 3. 통계 API
- [ ] **GET /api/admin/subscriptions/stats**: 구독 통계
  - 플랜별 구독자 수
  - 월별 매출
  - 이탈률

---

## 📋 image 파일 기준 기능 요구사항

> ⚠️ **Note**: 별도로 제공된 image 파일이나 기획 문서가 있다면 여기에 추가로 정리 필요

### 확인 필요 사항
- [ ] UI/UX 플로우 다이어그램 확인
- [ ] 플랜별 상세 기능 명세
- [ ] 결제 플로우 확인
- [ ] 환불 정책 확인
- [ ] 알림 타이밍 및 내용

---

## 🗂️ 파일 구조

```
com.ipzy.domain.subscription/
├── controller/
│   └── SubscriptionController.java      ✅ 완료 (4개 API)
├── service/
│   ├── SubscriptionService.java         ✅ 완료 (MVP 기능)
│   └── SubscriptionPlanService.java     ✅ 완료
├── repository/
│   ├── SubscriptionRepository.java      ✅ 완료
│   └── SubscriptionPlanRepository.java  ✅ 완료
├── entity/
│   ├── Subscription.java                ✅ 완료
│   └── SubscriptionPlan.java            ✅ 완료
├── dto/
│   ├── Request/
│   │   └── CreateSubscriptionRequest.java  ✅ 완료
│   └── Response/
│       ├── SubscriptionPlanResponse.java   ✅ 완료
│       └── SubscriptionResponse.java       ✅ 완료
├── exception/
│   ├── SubscriptionErrorCode.java       ✅ 완료
│   └── SubscriptionException.java       ✅ 완료
└── config/
    └── SubscriptionDataInitializer.java ✅ 완료 (초기 데이터)

com.ipzy.domain.payment/
├── controller/
│   └── PaymentController.java           ❌ 미구현
├── service/
│   └── PaymentService.java              ❌ 미구현
├── repository/
│   └── PaymentRepository.java           ❌ 미구현
└── entity/
    └── Payment.java                     ✅ 완료
```

---

## 📝 다음 작업 우선순위

1. ✅ ~~구독 생성 API 구현~~ (완료)
2. ✅ ~~내 구독 조회 API 구현~~ (완료)
3. ✅ ~~구독 취소 API 구현~~ (완료)
4. **[HIGH]** 토스페이먼츠 결제 연동 (유료 플랜 활성화)
   - PaymentService 및 PaymentRepository 구현
   - 결제 요청/승인/실패 API
   - 웹훅 처리
5. **[MEDIUM]** 구독 만료 스케줄러 구현
   - 자동 만료 처리
   - 자동 갱신 처리
6. **[LOW]** 관리자 기능 구현
   - 플랜 관리 API
   - 구독 관리 API
   - 통계 API

---

## 💡 참고사항

### 주요 구현 특징
- **자동 FREE 플랜 생성**: 구독이 없는 사용자 조회 시 자동으로 FREE 플랜 구독 생성 (7일)
- **플랜 변경 지원**: 기존 구독을 새로운 플랜으로 업데이트 가능 (`updatePlan` 메서드)
- **자동 만료 처리**: 조회 시 만료된 구독 자동 감지 및 EXPIRED 상태 전환
- **유료 플랜 결제 대기**: 유료 플랜 선택 시 PENDING 상태로 생성, 결제 완료 후 ACTIVE 전환 예정

### 현재 구독 플로우 (Phase 1)
```
신규 가입 → FREE (7일, ACTIVE)
         ↓
      7일 경과
         ↓
    FREE (EXPIRED)
         ↓
  사용자가 BASIC/PRO 선택
         ↓
  BASIC/PRO (PENDING) → 결제 완료 대기
```

### 향후 개선 예정 (Phase 2)
> **중요:** 결제 구현 이후 추가될 기능

**결제 실패 시 다운그레이드:**
- BASIC/PRO 결제 실패 → FREE (영구, 기능 제한) 플랜으로 자동 다운그레이드
- FREE_TRIAL (7일) 과 FREE (영구) 플랜 분리
  - FREE_TRIAL: 상품 10개, 추천 5개 (1회 한정)
  - FREE: 상품 5개, 추천 3개 (영구, 다운그레이드 대상)

**참고 문서:**
- [상세 구독 플로우](./SUBSCRIPTION_FLOW.md)

### 플랜별 결제 처리
| 플랜 | 가격 | 결제 필요 | 처리 방식 |
|------|------|-----------|-----------|
| Free | 0원 | ❌ | 즉시 ACTIVE |
| Basic | 9,900원 | ✅ | PENDING → 결제 → ACTIVE |
| Pro | 19,900원 | ✅ | PENDING → 결제 → ACTIVE |

### 구독 상태 전이
```
PENDING → ACTIVE → EXPIRED
   ↓        ↓
CANCELLED  CANCELLED
```

### 관련 문서
- [토스페이먼츠 API 문서](https://docs.tosspayments.com/reference)
- [Swagger UI](http://localhost:8080/swagger-ui/index.html)

---

**Last Updated**: 2025-12-15
**Status**: MVP Phase 1 완료 ✅ / Phase 2 준비 중