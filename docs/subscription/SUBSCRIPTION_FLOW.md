# 구독 플로우 및 상태 전이

## 📊 현재 구현된 플로우 (Phase 1)

### 신규 가입 시나리오

```
┌─────────────────────────────────────────────────────────────┐
│ 신규 사용자 가입 (OAuth2 로그인)                              │
└───────────────────┬─────────────────────────────────────────┘
                    ↓
        ┌───────────────────────┐
        │ FREE 플랜 자동 부여    │
        │ - 기간: 7일           │
        │ - 상태: ACTIVE        │
        │ - 기능: 제한됨        │
        └───────────┬───────────┘
                    ↓
        ┌───────────┴───────────┐
        │   7일 무료 체험 기간   │
        │  (상품 10개 제한 등)  │
        └───────────┬───────────┘
                    ↓
        ┌───────────┴────────────┐
        ↓                        ↓
   [7일 경과]            [유료 플랜 선택]
        ↓                        ↓
   FREE (EXPIRED)          BASIC/PRO (PENDING)
                                 ↓
                          [결제 완료 대기]
                                 ↓
                          BASIC/PRO (ACTIVE)
```

### 플랜 변경 시나리오

```
현재 구독: FREE (ACTIVE)
    ↓
사용자가 BASIC 선택
    ↓
기존 구독 업데이트 (updatePlan)
    ↓
FREE (ACTIVE) → BASIC (PENDING)
    ↓
결제 완료 (TODO: Phase 2)
    ↓
BASIC (PENDING) → BASIC (ACTIVE)
```

### 구독 만료 시나리오

```
BASIC/PRO (ACTIVE)
    ↓
endDate 경과
    ↓
스케줄러 또는 조회 시 expireIfNeeded() 실행
    ↓
BASIC/PRO (EXPIRED)
    ↓
사용자 다음 조회 시
    ↓
⚠️ 현재: 구독 없음 상태 (서비스 이용 불가)
⚠️ TODO: FREE (영구) 플랜으로 자동 다운그레이드
```

---

## 🚧 향후 구현 예정 플로우 (Phase 2+)

### **중요: 결제 실패 시 다운그레이드**

> **TODO (Phase 2):** 결제 구현 이후 추가될 기능
> Basic/Pro 결제 실패 시 영구적으로 기능 제한하는 FREE 플랜으로 다운그레이드

```
┌─────────────────────────────────────────────────────────────┐
│ Phase 2: Freemium 모델 (FREE_TRIAL + FREE 분리)             │
└─────────────────────────────────────────────────────────────┘

신규 가입
    ↓
FREE_TRIAL (7일, 1회 한정)
- 상품 10개
- 추천 5개
- AI 스타일링 ✗
    ↓
┌───┴────┐
↓        ↓
7일 경과  유료 전환
↓        ↓
FREE     BASIC/PRO (PENDING)
(영구)        ↓
- 상품 5개     결제 완료
- 추천 3개         ↓
- 제한 강화    BASIC/PRO (ACTIVE)
                   ↓
              ┌────┴────┐
              ↓         ↓
          결제 실패   구독 만료
              ↓         ↓
         BASIC (CANCELLED) or EXPIRED
              ↓         ↓
              └────┬────┘
                   ↓
            FREE (영구) 다운그레이드
            - 상품 5개
            - 추천 3개
            - 기본 기능만 제공
```

### 플랜 구조 (Phase 2)

| 플랜 | 기간 | 가격 | 상품 | 추천 | AI | 설명 |
|------|------|------|------|------|----|----|
| **FREE_TRIAL** | 7일 | 0원 | 10개 | 5개 | ✗ | 최초 1회 무료 체험 |
| **FREE** | 영구 | 0원 | **5개** | **3개** | ✗ | 기본 무료 플랜 (다운그레이드 대상) |
| **BASIC** | 월 | 9,900원 | 50개 | 20개 | ✓ | 개인용 |
| **PRO** | 월 | 19,900원 | 무제한 | 무제한 | ✓ | 프로페셔널용 |

---

## 🔄 상태 전이 다이어그램

### 현재 구현 (Phase 1)

```
           ┌─────────┐
신규 가입 → │ ACTIVE  │ ← FREE 플랜 (7일)
           └────┬────┘
                │
       ┌────────┼────────┐
       ↓        ↓        ↓
    만료됨   유료선택   취소
       ↓        ↓        ↓
   EXPIRED   PENDING  CANCELLED
               ↓
          결제 완료
               ↓
            ACTIVE
```

### 향후 구현 (Phase 2)

```
           ┌──────────┐
신규 가입 → │ ACTIVE   │ ← FREE_TRIAL (7일)
           │(TRIAL)   │
           └────┬─────┘
                │
       ┌────────┼────────┐
       ↓        ↓        ↓
    7일경과  유료선택   취소
       ↓        ↓        ↓
   ┌─────┐  PENDING  CANCELLED
   │ACTIVE│     ↓
   │(FREE)│  결제완료
   └──↑──┘     ↓
      │    ┌─────┐
      │    │ACTIVE│
      │    │(PAID)│
      │    └──┬──┘
      │       │
      │  ┌────┼─────┐
      │  ↓    ↓     ↓
      └결제  만료  취소
       실패   ↓     ↓
          EXPIRED CANCELLED
             ↓     ↓
          FREE 다운그레이드
```

---

## 📋 Unique Constraint 규칙

### 비즈니스 규칙

**사용자는 여러 구독을 가질 수 있되, ACTIVE 또는 PENDING 상태는 동시에 하나만 가능**

```sql
-- PostgreSQL Partial Unique Index
CREATE UNIQUE INDEX uk_subscription_user_valid_status
ON subscriptions (user_id)
WHERE status IN ('ACTIVE', 'PENDING');
```

### 허용되는 구독 조합

| 상태 조합 | 허용 여부 | 설명 |
|----------|----------|------|
| ACTIVE (1개) | ✅ | 현재 활성 구독 |
| PENDING (1개) | ✅ | 결제 대기 중 |
| ACTIVE (1) + PENDING (1) | ❌ | **불가능 (Unique Constraint)** |
| EXPIRED (여러개) | ✅ | 히스토리 유지 가능 |
| CANCELLED (여러개) | ✅ | 취소 기록 유지 가능 |
| ACTIVE (1) + EXPIRED (N) | ✅ | 현재 구독 + 과거 히스토리 |

### 예시 시나리오

```
사용자 A의 구독 히스토리:

| ID | Plan  | Status    | Start      | End        |
|----|-------|-----------|------------|------------|
| 1  | FREE  | EXPIRED   | 2025-01-01 | 2025-01-08 | ✅ 히스토리
| 2  | BASIC | CANCELLED | 2025-01-08 | 2025-02-08 | ✅ 히스토리
| 3  | PRO   | ACTIVE    | 2025-02-10 | 2025-03-10 | ✅ 현재 구독

Unique Constraint: user_id=A의 ACTIVE/PENDING은 1개뿐 (ID=3)
```

---

## 🔧 구현 체크리스트

### Phase 1 (현재) ✅
- [x] FREE 플랜 자동 부여 (7일)
- [x] BASIC/PRO 선택 시 PENDING 상태
- [x] updatePlan으로 구독 전환
- [x] Unique Constraint (ACTIVE/PENDING 하나만)
- [x] 구독 취소 기능
- [x] 만료 감지 (expireIfNeeded)

### Phase 2 (TODO: 결제 구현 후)
- [ ] **FREE_TRIAL과 FREE 플랜 분리**
  - [ ] FREE_TRIAL: 7일, 상품 10개
  - [ ] FREE: 영구, 상품 5개 (더 제한적)
- [ ] **결제 실패 시 다운그레이드**
  - [ ] PaymentService.handlePaymentFailure()
  - [ ] BASIC/PRO (PENDING) → FREE (ACTIVE)
  - [ ] 알림: "결제 실패, 무료 플랜으로 전환되었습니다"
- [ ] **구독 만료 시 다운그레이드**
  - [ ] 스케줄러: BASIC/PRO (EXPIRED) → FREE (ACTIVE)
  - [ ] 알림: "구독 만료, 무료 플랜으로 전환되었습니다"
- [ ] **User 플래그 추가**
  - [ ] hasUsedFreeTrial: boolean
  - [ ] FREE_TRIAL 중복 사용 방지

---

## 💡 중요 노트

### 현재 제약사항 (Phase 1)

1. **FREE 플랜 = 7일 무료 체험**
   - 현재는 FREE_TRIAL과 FREE를 구분하지 않음
   - 7일 후 만료 시 구독 없음 상태가 됨

2. **결제 구현 전**
   - BASIC/PRO 선택 시 PENDING 상태로만 변경
   - 실제 결제 처리 없음
   - ACTIVE 전환 수동 처리 필요

3. **만료 후 처리 없음**
   - 만료 시 EXPIRED 상태로만 변경
   - 자동 다운그레이드 없음
   - 사용자가 수동으로 새 플랜 선택 필요

### 향후 개선 방향 (Phase 2+)

1. **Freemium 모델 완성**
   ```
   FREE_TRIAL (7일) → FREE (영구) or BASIC/PRO (유료)
   유료 만료/실패 → FREE (영구) 다운그레이드
   ```

2. **자동화**
   - 스케줄러로 만료 구독 일괄 처리
   - 다운그레이드 자동 실행
   - 이메일 알림 발송

3. **사용자 경험 개선**
   - 만료 예정 알림 (7일 전, 1일 전)
   - 다운그레이드 전 경고
   - 기능 제한 안내

---

**Last Updated**: 2025-12-15
**Current Phase**: Phase 1 (구독 기본 기능)
**Next Phase**: Phase 2 (결제 연동 및 다운그레이드)