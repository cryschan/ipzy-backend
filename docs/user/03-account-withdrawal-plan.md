# 계정 탈퇴 구현 계획

## 개요

계정 탈퇴는 사용자가 서비스를 떠날 때 **개인정보 보호법과 비즈니스 요구사항**을 동시에 충족해야 합니다.

```text
탈퇴 요청 → 활성 구독 확인 → OAuth 연동 해제 → 연관 데이터 처리 → 계정 비활성화
    ↑              ↑              ↑                ↑                ↑
    └──────────────┴──────────────┴────────────────┴────────────────┘
                              User 도메인 (탈퇴 처리)
```

---

## User 연관 엔티티 현황

### ERD (User 중심)

```text
┌─────────────────────────────────────────────────────────────────┐
│                           USER                                   │
│  id, email, name, phone, provider, providerId                    │
│  status (ACTIVE, SUSPENDED, DELETED), deletedAt                  │
└─────────────────────────────────────────────────────────────────┘
          │
          │ FK references (1:N)
          │
    ┌─────┼─────┬──────────┬──────────┬──────────┬──────────┐
    │     │     │          │          │          │          │
    ▼     ▼     ▼          ▼          ▼          ▼          ▼
Activity Admin  Saved    Payment   Quiz     Recommend  Subscription
  Log   Audit   Outfit            Session    ation
        Log
```

### 연관 엔티티 상세

| 도메인 | 엔티티 | FK Nullable | 데이터 성격 | 법적 보존 의무 |
|--------|--------|:-----------:|-------------|:--------------:|
| Activity | ActivityLog | X | 사용자 활동 로그 | X |
| Admin | AdminAuditLog | X | 관리자 작업 로그 | O (감사) |
| Outfit | SavedOutfit | X | 저장된 코디 | X |
| Payment | Payment | X | 결제 내역 | O (5년) |
| Quiz | QuizSession | O | 퀴즈 응답 | X |
| Recommendation | Recommendation | O | AI 추천 결과 | X |
| Subscription | Subscription | X | 구독 정보 | O (5년) |

---

## 탈퇴 전략 선택지

### Option A: Soft Delete (권장)

**설명:** User.status를 `DELETED`로 변경, deletedAt 기록

```java
user.setStatus(UserStatus.DELETED);
user.setDeletedAt(LocalDateTime.now());
// 개인정보 마스킹
user.setEmail("deleted_" + user.getId() + "@ipzy.com");
user.setName("탈퇴한 사용자");
user.setPhone(null);
user.setProfileImageUrl(null);
```

| 장점 | 단점 |
|------|------|
| 복구 가능 (유예 기간) | 저장 공간 차지 |
| 연관 데이터 무결성 유지 | 주기적 정리 필요 |
| 통계/분석 데이터 보존 | |

### Option B: Hard Delete

**설명:** User 및 연관 데이터 물리적 삭제

| 장점 | 단점 |
|------|------|
| 완전한 삭제 | 복구 불가 |
| 저장 공간 절약 | FK 제약 처리 복잡 |
| GDPR 완전 준수 | 통계 데이터 손실 |

### Option C: 익명화 (Anonymization)

**설명:** 개인정보만 삭제/마스킹, 연관 데이터는 익명으로 보존

```java
// User 익명화
user.setEmail("anon_" + UUID.randomUUID() + "@ipzy.com");
user.setName("익명");
user.setPhone(null);
user.setProviderId(null);

// 연관 데이터의 FK를 null로 설정 (nullable인 경우)
quizSession.setUser(null);
recommendation.setUser(null);
```

| 장점 | 단점 |
|------|------|
| 통계 보존 + 규정 준수 | 구현 복잡도 증가 |
| 재가입 시 중복 방지 용이 | 추가 익명 ID 관리 필요 |

---

## 탈퇴 전 필수 확인 사항

### 1. 활성 구독 확인

```java
// 활성 구독이 있으면 탈퇴 진행 불가 또는 경고
Optional<Subscription> activeSubscription = subscriptionRepository
    .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);

if (activeSubscription.isPresent()) {
    throw new WithdrawalException(WithdrawalErrorCode.ACTIVE_SUBSCRIPTION_EXISTS);
}
```

**처리 방안:**
- [ ] 구독 취소 후 탈퇴 유도
- [ ] 잔여 기간 환불 정책 결정
- [ ] 구독 자동 갱신 해제

### 2. 미완료 결제 확인

```java
// PENDING 상태 결제가 있으면 완료 후 탈퇴
List<Payment> pendingPayments = paymentRepository
    .findByUserIdAndStatus(userId, PaymentStatus.PENDING);

if (!pendingPayments.isEmpty()) {
    throw new WithdrawalException(WithdrawalErrorCode.PENDING_PAYMENT_EXISTS);
}
```

### 3. 관리자 권한 확인

```java
// 관리자는 일반 탈퇴 불가 (별도 프로세스)
if (user.getRole() == UserRole.ADMIN) {
    throw new WithdrawalException(WithdrawalErrorCode.ADMIN_CANNOT_WITHDRAW);
}
```

---

## OAuth 연동 해제

### 카카오 연동 해제

```java
// 카카오 연결 끊기 API 호출
// POST https://kapi.kakao.com/v1/user/unlink
// Authorization: Bearer {access_token}

public void unlinkKakao(String accessToken) {
    restClient.post()
        .uri("https://kapi.kakao.com/v1/user/unlink")
        .header("Authorization", "Bearer " + accessToken)
        .retrieve()
        .toBodilessEntity();
}
```

**주의사항:**
- 세션에서 OAuth2 토큰 조회 필요
- 토큰 만료 시 연동 해제 스킵 (이미 무효화됨)

### 네이버 연동 해제

```java
// 네이버 연결 끊기 API 호출
// POST https://nid.naver.com/oauth2.0/token
// grant_type=delete, client_id, client_secret, access_token

public void unlinkNaver(String accessToken) {
    restClient.post()
        .uri("https://nid.naver.com/oauth2.0/token")
        .body(Map.of(
            "grant_type", "delete",
            "client_id", naverClientId,
            "client_secret", naverClientSecret,
            "access_token", accessToken
        ))
        .retrieve()
        .toBodilessEntity();
}
```

### 구글 연동 해제

```java
// 구글 토큰 해지 API 호출
// POST https://oauth2.googleapis.com/revoke?token={access_token}

public void unlinkGoogle(String accessToken) {
    restClient.post()
        .uri("https://oauth2.googleapis.com/revoke?token=" + accessToken)
        .retrieve()
        .toBodilessEntity();
}
```

---

## 연관 데이터 처리 방안

### 엔티티별 처리 전략

| 엔티티 | 처리 방법 | 사유 |
|--------|----------|------|
| ActivityLog | 삭제 또는 익명화 | 개인 활동 기록 |
| AdminAuditLog | 보존 | 감사 로그 (법적 의무) |
| SavedOutfit | 삭제 | 개인 데이터 |
| Payment | 보존 (5년) | 전자상거래법, 세법 |
| QuizSession | FK null 설정 | 통계 보존 (익명화) |
| Recommendation | FK null 설정 | 통계 보존 (익명화) |
| Subscription | 보존 (5년) | 거래 기록 |

### 구현 코드

```java
@Transactional
public void processWithdrawal(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserException.notFound());

    // 1. 삭제 대상
    savedOutfitRepository.deleteByUserId(userId);
    activityLogRepository.deleteByUserId(userId);  // 또는 익명화

    // 2. 익명화 대상 (FK nullable)
    quizSessionRepository.setUserIdNull(userId);
    recommendationRepository.setUserIdNull(userId);

    // 3. 보존 대상 (Payment, Subscription, AdminAuditLog)
    // - 아무 처리 안 함 (FK 유지)

    // 4. User 상태 변경 및 개인정보 마스킹
    user.withdraw();
}
```

### User.withdraw() 메서드

```java
public void withdraw() {
    this.status = UserStatus.DELETED;
    this.deletedAt = LocalDateTime.now();

    // 개인정보 마스킹
    this.email = "deleted_" + this.id + "_" + System.currentTimeMillis() + "@ipzy.com";
    this.name = "탈퇴한 사용자";
    this.phone = null;
    this.profileImageUrl = null;
    this.providerId = "deleted_" + this.id;
    this.preferences = null;
    this.stylePreference = null;
}
```

---

## 세션/인증 무효화

```java
@Transactional
public void invalidateAllSessions(Long userId) {
    // Spring Session을 사용하는 경우
    // Redis 또는 JDBC에서 해당 사용자의 모든 세션 삭제

    // 현재 요청의 세션 무효화
    SecurityContextHolder.clearContext();
    request.getSession().invalidate();
}
```

---

## API 설계

### 탈퇴 API

```http
DELETE /api/v1/users/me
Authorization: Required (세션)

Request Body (선택):
{
    "reason": "서비스 불만족",      // 탈퇴 사유 (통계용)
    "feedback": "추가 의견..."      // 추가 피드백
}

Response:
- 204 No Content (성공)
- 400 Bad Request (활성 구독 존재)
- 401 Unauthorized (미인증)
```

### 탈퇴 확인 API (선택)

```http
GET /api/v1/users/me/withdrawal-check
Authorization: Required

Response:
{
    "canWithdraw": false,
    "blockers": [
        {
            "type": "ACTIVE_SUBSCRIPTION",
            "message": "활성 구독이 있습니다. 구독 취소 후 탈퇴해주세요.",
            "subscriptionEndDate": "2024-02-15"
        }
    ]
}
```

---

## 구현 순서

### Step 1: Exception 생성

| 파일 | 설명 |
|------|------|
| `WithdrawalErrorCode.java` | 탈퇴 관련 에러 코드 |
| `WithdrawalException.java` | 탈퇴 관련 예외 |

**에러 코드:**
```java
public enum WithdrawalErrorCode {
    ACTIVE_SUBSCRIPTION_EXISTS(400, "W001", "활성 구독이 있어 탈퇴할 수 없습니다"),
    PENDING_PAYMENT_EXISTS(400, "W002", "처리 중인 결제가 있어 탈퇴할 수 없습니다"),
    ADMIN_CANNOT_WITHDRAW(400, "W003", "관리자는 일반 탈퇴할 수 없습니다"),
    ALREADY_WITHDRAWN(400, "W004", "이미 탈퇴한 계정입니다");
}
```

### Step 2: User Entity 수정

```java
// User.java에 withdraw() 메서드 추가
public void withdraw() { ... }

// UserStatus enum에 DELETED 상태 확인
public enum UserStatus {
    ACTIVE, SUSPENDED, DELETED
}
```

### Step 3: Repository 쿼리 추가

```java
// UserRepository
@Modifying
@Query("UPDATE QuizSession qs SET qs.user = null WHERE qs.user.id = :userId")
void nullifyQuizSessionUser(@Param("userId") Long userId);

// SavedOutfitRepository
void deleteByUserId(Long userId);

// ActivityLogRepository
void deleteByUserId(Long userId);
```

### Step 4: WithdrawalService 구현

```java
@Service
@RequiredArgsConstructor
@Transactional
public class WithdrawalService {

    public void withdraw(Long userId, WithdrawalRequest request) {
        // 1. 탈퇴 가능 여부 확인
        validateWithdrawal(userId);

        // 2. OAuth 연동 해제
        unlinkOAuthProvider(userId);

        // 3. 연관 데이터 처리
        processRelatedData(userId);

        // 4. 사용자 탈퇴 처리
        User user = userRepository.findById(userId).orElseThrow();
        user.withdraw();

        // 5. 탈퇴 사유 저장 (통계용)
        saveWithdrawalReason(userId, request);

        // 6. 세션 무효화
        invalidateSessions(userId);
    }
}
```

### Step 5: Controller 추가

```java
@DeleteMapping("/me")
@Operation(summary = "계정 탈퇴")
public ResponseEntity<Void> withdraw(
    @AuthenticationPrincipal CustomUserPrincipal principal,
    @RequestBody(required = false) WithdrawalRequest request
) {
    withdrawalService.withdraw(principal.getUserId(), request);
    return ResponseEntity.noContent().build();
}
```

### Step 6: 테스트 작성

```java
@Test
void 활성_구독이_있으면_탈퇴_실패() { ... }

@Test
void 탈퇴_성공_시_개인정보_마스킹() { ... }

@Test
void 탈퇴_후_연관_데이터_처리_확인() { ... }
```

---

## 유예 기간 정책 (선택)

### 즉시 삭제 vs 유예 기간

| 방식 | 설명 | 장점 | 단점 |
|------|------|------|------|
| 즉시 처리 | 탈퇴 요청 시 바로 처리 | 간단한 구현 | 실수로 탈퇴 시 복구 불가 |
| 유예 기간 | 30일 후 완전 삭제 | 복구 가능 | 배치 작업 필요 |

### 유예 기간 구현 시 추가 작업

```java
// 1. 탈퇴 요청 시
user.setStatus(UserStatus.PENDING_DELETION);
user.setDeletedAt(LocalDateTime.now());  // 요청 시점 기록

// 2. 스케줄러로 30일 후 완전 삭제
@Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시
public void processExpiredWithdrawals() {
    LocalDateTime threshold = LocalDateTime.now().minusDays(30);
    List<User> expiredUsers = userRepository
        .findByStatusAndDeletedAtBefore(UserStatus.PENDING_DELETION, threshold);

    for (User user : expiredUsers) {
        processWithdrawal(user.getId());
    }
}

// 3. 복구 API
@PostMapping("/me/restore")
public void restore(@AuthenticationPrincipal CustomUserPrincipal principal) {
    // 유예 기간 내 복구
}
```

---

## 재가입 정책

### 동일 계정 재가입 방지 (선택)

```java
// providerId를 해시로 보관하여 재가입 제한
public void withdraw() {
    // ...
    this.providerIdHash = hashProviderId(this.providerId);
    this.providerId = "deleted_" + this.id;
}

// 회원가입 시 확인
public User registerOrLogin(String provider, String providerId) {
    String hash = hashProviderId(providerId);

    Optional<User> recentlyDeleted = userRepository
        .findByProviderIdHashAndDeletedAtAfter(hash, LocalDateTime.now().minusDays(30));

    if (recentlyDeleted.isPresent()) {
        throw new AuthException(AuthErrorCode.RECENTLY_WITHDRAWN);
    }
    // ...
}
```

---

## 법적 요구사항 정리

| 법률 | 요구사항 | 적용 대상 |
|------|----------|----------|
| 개인정보보호법 | 탈퇴 후 지체 없이 파기 | 모든 개인정보 |
| 전자상거래법 | 거래 기록 5년 보존 | Payment, Subscription |
| 세법 | 재무 기록 5~7년 보존 | Payment |
| GDPR (해당 시) | 삭제권(Right to Erasure) | 모든 개인정보 |

---

## 파일 체크리스트

| 순서 | 작업 | 파일 경로 | 상태 |
|:----:|------|----------|:----:|
| 1 | 생성 | `domain/user/exception/WithdrawalErrorCode.java` | |
| 2 | 생성 | `domain/user/exception/WithdrawalException.java` | |
| 3 | 수정 | `domain/user/entity/User.java` (withdraw 메서드) | |
| 4 | 수정 | `domain/user/repository/UserRepository.java` | |
| 5 | 생성 | `domain/user/dto/WithdrawalRequest.java` | |
| 6 | 생성 | `domain/user/dto/WithdrawalCheckResponse.java` | |
| 7 | 생성 | `domain/user/service/WithdrawalService.java` | |
| 8 | 수정 | `domain/user/controller/UserController.java` | |
| 9 | 생성 | `domain/user/service/WithdrawalServiceTest.java` | |
| 10 | (선택) | OAuth 연동 해제 클라이언트 | |

---

## 의사결정 필요 사항

구현 전 결정이 필요한 사항:

1. **탈퇴 전략**: Soft Delete / Hard Delete / 익명화?
2. **유예 기간**: 즉시 처리 / 30일 유예?
3. **재가입 제한**: 제한 없음 / 30일 제한?
4. **OAuth 연동 해제**: 필수 / 선택?
5. **ActivityLog 처리**: 삭제 / 익명화 / 보존?

---

## 법적 근거 상세

### 전자상거래법 (전자상거래 등에서의 소비자보호에 관한 법률)

**근거:** 제6조 (거래기록의 보존 등)

> 사업자는 전자상거래 및 통신판매에서의 표시·광고, 계약내용 및 그 이행 등 거래에 관한 기록을 일정 기간 보존하여야 한다.

| 보존 대상 | 보존 기간 | 해당 엔티티 |
|----------|:---------:|------------|
| 표시·광고에 관한 기록 | 6개월 | - |
| 계약 또는 청약철회에 관한 기록 | 5년 | Subscription |
| 대금결제 및 재화 등의 공급에 관한 기록 | 5년 | Payment |
| 소비자 불만 또는 분쟁처리에 관한 기록 | 3년 | - |

**결론:** Payment, Subscription 데이터는 **거래 증빙 및 소비자 분쟁 대응**을 위해 탈퇴 후에도 **5년간 보존**되어야 한다.

---

### 세법 (국세기본법, 법인세법, 부가가치세법)

**근거:** 국세기본법 제85조의3 (장부 등의 비치와 보존)

> 납세자는 각 세법에서 규정하는 장부와 증거서류를 그 거래사실이 속하는 과세기간에 대한 해당 국세의 법정신고기한이 지난 날부터 5년간 보존하여야 한다.

| 보존 대상 | 보존 기간 | 근거 법령 |
|----------|:---------:|----------|
| 거래에 관한 장부 및 증거서류 | 5년 | 국세기본법 제85조의3 |
| 세금계산서 | 5년 | 부가가치세법 제71조 |
| 법인 장부·서류 (대기업) | 10년 | 법인세법 시행령 제158조 |

**결론:** Payment 데이터는 **세무조사 대응 및 세금 신고 증빙**을 위해 탈퇴 후에도 **5년간 보존**되어야 한다.

---

### 요약: 탈퇴 시 데이터 처리 원칙

```text
┌─────────────────────────────────────────────────────────────────┐
│  개인정보보호법: 탈퇴 즉시 파기 원칙                             │
│  ↓                                                               │
│  예외: 다른 법령에 보존 의무가 있는 경우                         │
│  ├─ 전자상거래법: 거래 기록 5년 보존                             │
│  └─ 세법: 재무 증빙 5년 보존                                     │
└─────────────────────────────────────────────────────────────────┘
```

| 데이터 구분 | 처리 방법 | 법적 근거 |
|------------|----------|----------|
| 개인정보 (이름, 이메일, 전화번호) | 즉시 삭제/마스킹 | 개인정보보호법 |
| 결제 기록 (Payment) | 5년 보존 | 전자상거래법, 세법 |
| 구독 기록 (Subscription) | 5년 보존 | 전자상거래법 |
| 관리자 감사 로그 (AdminAuditLog) | 영구 보존 | 내부 감사 정책 |
| 활동 로그, 저장 코디 등 | 즉시 삭제 | 개인정보보호법 |

**핵심:** 개인을 **식별할 수 없도록 마스킹**하되, 거래 사실 자체는 **법적 보존 기간 동안 유지**한다.
