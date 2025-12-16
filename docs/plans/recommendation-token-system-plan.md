# 추천 토큰 시스템 설계

## 개요

추천 서비스의 비용 관리와 사용자 경험을 위한 토큰 기반 시스템 설계

## 비즈니스 모델

```
┌─────────────────────────────────────────────────────┐
│                    추천 토큰 시스템                    │
├─────────────────────────────────────────────────────┤
│  1 토큰 = 1회 추천 (3개 코디 세트 제공)                │
│  1 세션 = 최대 3회 재시도 가능 (3토큰 소비)            │
├─────────────────────────────────────────────────────┤
│  무료: 회원가입 시 3토큰 제공                          │
│  유료: 구독 플랜에 따라 토큰 충전                      │
└─────────────────────────────────────────────────────┘
```

## 핵심 규칙

| 항목 | 규칙 |
|------|------|
| 토큰 단위 | 1토큰 = 1회 추천 |
| 추천 결과 | 1회 추천당 3개 코디 세트 |
| 재시도 | 같은 세션에서 최대 3회 |
| 무료 제공 | 회원가입 시 3토큰 |

---

## 도메인 구조

```
user (사용자)
  └── recommendationTokens (토큰 잔액)

token_transaction (토큰 거래 내역)
  └── 충전/사용 기록

subscription (구독) - Phase 2
  └── subscription_plan (플랜별 토큰 수량)

recommendation (추천)
  └── attemptNumber (재시도 번호: 1, 2, 3)
```

---

## 엔티티 변경 사항

### 1. User Entity (수정)

```java
@Entity
public class User {
    // 기존 필드...

    @Column(nullable = false)
    private Integer recommendationTokens = 0;

    public void addTokens(int amount) {
        this.recommendationTokens += amount;
    }

    public void useToken(int amount) {
        if (this.recommendationTokens < amount) {
            throw new IllegalStateException("토큰 부족");
        }
        this.recommendationTokens -= amount;
    }

    public boolean hasTokens() {
        return this.recommendationTokens > 0;
    }
}
```

### 2. Recommendation Entity (수정)

```java
@Entity
public class Recommendation {
    // 기존 필드...

    @Column(nullable = false)
    private Integer attemptNumber = 1;  // 1, 2, 3 (몇 번째 시도인지)
}
```

### 3. TokenTransaction Entity (신규)

```java
@Entity
@Table(name = "token_transactions")
public class TokenTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer amount;  // +3(충전), -1(사용)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenTransactionType type;

    private Long referenceId;  // 관련 추천/구독 ID

    @Column(nullable = false)
    private LocalDateTime createdAt;
}

public enum TokenTransactionType {
    SIGNUP_BONUS,      // 회원가입 보너스
    SUBSCRIPTION,      // 구독 충전
    PURCHASE,          // 개별 구매
    USE,               // 사용
    REFUND,            // 환불
    ADMIN_GRANT        // 관리자 지급
}
```

---

## Repository 변경

### RecommendationRepository (수정)

```java
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    // 기존 메서드...

    /**
     * 세션별 추천 횟수 조회 (재시도 제한 확인용)
     */
    long countBySessionId(Long sessionId);
}
```

### TokenTransactionRepository (신규)

```java
public interface TokenTransactionRepository extends JpaRepository<TokenTransaction, Long> {

    List<TokenTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
```

---

## Service 로직 변경

### RecommendationService

```java
@Transactional
public List<Recommendation> generateRecommendation(Long sessionId, Long currentUserId) {

    // 1. 세션 조회
    QuizSession session = findCompletedSession(sessionId);

    // 2. 사용자 조회
    User user = userService.findActiveUser(currentUserId);

    // 3. 익명 세션이면 연결
    if (session.isAnonymous()) {
        session.assignUser(user);
    } else {
        validateSessionOwnership(session, currentUserId);
    }

    // 4. 토큰 확인
    if (!user.hasTokens()) {
        throw RecommendationException.insufficientTokens();
    }

    // 5. 재시도 횟수 확인 (최대 3회)
    long attemptCount = recommendationRepository.countBySessionId(sessionId);
    if (attemptCount >= 3) {
        throw RecommendationException.retryLimitExceeded(sessionId);
    }

    // 6. 토큰 차감
    user.useToken(1);
    tokenTransactionService.recordUsage(user, sessionId);

    // 7. AI 추천 요청
    RecommendationRequest request = RecommendationRequest.of(session);
    RecommendationResponse response = aiClient.requestRecommendation(request);

    // 8. Entity 변환 (attemptNumber 포함)
    int attemptNumber = (int) attemptCount + 1;
    List<Recommendation> recommendations = convertToEntities(session, user, response, attemptNumber);

    // 9. 저장
    return recommendationRepository.saveAll(recommendations);
}
```

### UserService (회원가입 시 토큰 지급)

```java
@Transactional
public User createUser(...) {
    User user = User.builder()
        // 기존 필드...
        .recommendationTokens(3)  // 가입 보너스
        .build();

    User savedUser = userRepository.save(user);

    // 토큰 거래 내역 기록
    tokenTransactionService.recordSignupBonus(savedUser, 3);

    return savedUser;
}
```

---

## 에러 코드

### RecommendationErrorCode

```java
public enum RecommendationErrorCode implements ErrorCode {

    // 기존 에러 코드...

    // 토큰 관련
    INSUFFICIENT_TOKENS(HttpStatus.PAYMENT_REQUIRED, "REC_009", "추천 토큰이 부족합니다"),
    RETRY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "REC_010", "재시도 횟수를 초과했습니다 (최대 3회)");
}
```

### RecommendationException

```java
public static RecommendationException insufficientTokens() {
    return new RecommendationException(RecommendationErrorCode.INSUFFICIENT_TOKENS);
}

public static RecommendationException retryLimitExceeded(Long sessionId) {
    return new RecommendationException(
        RecommendationErrorCode.RETRY_LIMIT_EXCEEDED,
        "세션 ID: " + sessionId
    );
}
```

---

## API 변경

### 추천 생성 응답 (수정)

```json
{
  "success": true,
  "data": [
    {
      "recommendationId": 5,
      "attemptNumber": 2,
      "displayOrder": 1,
      "occasion": "데이트",
      "season": "봄",
      "style": "캐주얼",
      "reason": "밝은 색감의 캐주얼 룩입니다.",
      "totalPrice": 237000,
      "styleBoardUrl": "https://example.com/style1.jpg",
      "items": [...]
    }
  ],
  "meta": {
    "remainingTokens": 2,
    "remainingRetries": 1,
    "maxRetries": 3
  }
}
```

### 토큰 조회 API (신규)

```
GET /api/users/me/tokens
```

```json
{
  "success": true,
  "data": {
    "balance": 2,
    "transactions": [
      {
        "id": 1,
        "amount": 3,
        "type": "SIGNUP_BONUS",
        "createdAt": "2025-01-01T00:00:00"
      },
      {
        "id": 2,
        "amount": -1,
        "type": "USE",
        "referenceId": 5,
        "createdAt": "2025-01-02T10:00:00"
      }
    ]
  }
}
```

---

## 플로우 예시

```
[회원가입]
토큰: 0 → 3 (가입 보너스)

[세션 18 - 첫 번째 추천]
├── 토큰 확인: 3개 ✅
├── 재시도 횟수: 0/3 ✅
├── 토큰 차감: 3 → 2
└── 추천 생성 (attemptNumber: 1)

[세션 18 - 재추천 요청]
├── 토큰 확인: 2개 ✅
├── 재시도 횟수: 1/3 ✅
├── 토큰 차감: 2 → 1
└── 추천 생성 (attemptNumber: 2)

[세션 18 - 재추천 요청]
├── 토큰 확인: 1개 ✅
├── 재시도 횟수: 2/3 ✅
├── 토큰 차감: 1 → 0
└── 추천 생성 (attemptNumber: 3)

[세션 18 - 재추천 요청]
├── 토큰 확인: 0개 ❌
└── 402 "추천 토큰이 부족합니다"

[세션 19 - 새 세션 추천 (토큰 충전 후)]
├── 토큰 확인: 5개 ✅
├── 재시도 횟수: 0/3 ✅
├── 토큰 차감: 5 → 4
└── 추천 생성 (attemptNumber: 1)
```

---

## 구현 우선순위

### Phase 1: MVP (토큰 기본 기능)

| 작업 | 파일 | 설명 |
|------|------|------|
| 1 | `User.java` | `recommendationTokens` 필드 추가 |
| 2 | `Recommendation.java` | `attemptNumber` 필드 추가 |
| 3 | `RecommendationRepository.java` | `countBySessionId` 메서드 추가 |
| 4 | `RecommendationErrorCode.java` | 토큰/재시도 에러 코드 추가 |
| 5 | `RecommendationException.java` | 팩토리 메서드 추가 |
| 6 | `RecommendationService.java` | 토큰 차감 & 재시도 로직 |
| 7 | `CustomOAuth2UserService.java` | 가입 시 토큰 지급 |
| 8 | DB Migration | `ALTER TABLE users ADD recommendation_tokens` |

### Phase 2: 토큰 관리

| 작업 | 파일 | 설명 |
|------|------|------|
| 1 | `TokenTransaction.java` | 거래 내역 엔티티 |
| 2 | `TokenTransactionRepository.java` | Repository |
| 3 | `TokenTransactionService.java` | 충전/사용 기록 |
| 4 | `UserController.java` | 토큰 조회 API |

### Phase 3: 구독 연동

| 작업 | 파일 | 설명 |
|------|------|------|
| 1 | `SubscriptionPlan.java` | `monthlyTokens` 필드 |
| 2 | `SubscriptionService.java` | 구독 시 토큰 충전 |
| 3 | 스케줄러 | 월간 토큰 자동 충전 |

---

## DB 마이그레이션

### users 테이블

```sql
ALTER TABLE users
ADD COLUMN recommendation_tokens INTEGER NOT NULL DEFAULT 0;

-- 기존 사용자에게 3토큰 지급 (선택)
UPDATE users SET recommendation_tokens = 3 WHERE recommendation_tokens = 0;
```

### recommendations 테이블

```sql
ALTER TABLE recommendations
ADD COLUMN attempt_number INTEGER NOT NULL DEFAULT 1;
```

### token_transactions 테이블 (신규)

```sql
CREATE TABLE token_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    amount INTEGER NOT NULL,
    type VARCHAR(50) NOT NULL,
    reference_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_token_transactions_user_id ON token_transactions(user_id);
```

---

## 테스트 시나리오

### 1. 토큰 부족 테스트
```
Given: 사용자 토큰 0개
When: 추천 생성 요청
Then: 402 REC_009 "추천 토큰이 부족합니다"
```

### 2. 재시도 제한 테스트
```
Given: 세션에 이미 3개 추천 존재
When: 같은 세션 추천 생성 요청
Then: 429 REC_010 "재시도 횟수를 초과했습니다"
```

### 3. 정상 재시도 테스트
```
Given: 사용자 토큰 2개, 세션에 1개 추천 존재
When: 같은 세션 추천 생성 요청
Then: 200 OK, attemptNumber: 2, remainingTokens: 1
```

### 4. 회원가입 보너스 테스트
```
Given: 신규 사용자 가입
When: 가입 완료
Then: recommendationTokens = 3
```

---

## 관련 문서

- [추천 API 구현](/docs/api/03-recommendation-api-implementation.md)
- [구독 플랜 설계](/docs/subscription/) - TODO
