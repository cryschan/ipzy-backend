# 추천 재시도 기능 구현 계획

## 목표

현재 "1세션 = 1추천" 구조를 "1세션 = 최대 3추천"으로 변경

## 현재 vs 변경 후

| 항목 | 현재 | 변경 후 |
|------|------|---------|
| 중복 체크 | `existsBySessionId` → 있으면 409 | `countBySessionId` → 3회 이상이면 429 |
| 토큰 | 없음 | 매 추천마다 1토큰 차감 |
| 응답 | 추천 결과만 | + attemptNumber, remainingRetries |

---

## 구현 단계

### Step 1: Entity 수정

#### 1-1. User.java - 토큰 필드 추가

```java
// 추가할 필드
private Integer recommendationTokens = 0;

// 추가할 메서드
public void addTokens(int amount);
public void useToken(int amount);
public boolean hasTokens();
```

#### 1-2. Recommendation.java - attemptNumber 필드 추가

```java
// 추가할 필드
@Column(nullable = false)
private Integer attemptNumber = 1;
```

---

### Step 2: Repository 수정

#### 2-1. RecommendationRepository.java

```java
// 추가할 메서드
long countBySessionId(Long sessionId);
```

---

### Step 3: ErrorCode 추가

#### 3-1. RecommendationErrorCode.java

```java
// 추가할 에러 코드
INSUFFICIENT_TOKENS(HttpStatus.PAYMENT_REQUIRED, "REC_009", "추천 토큰이 부족합니다"),
RETRY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "REC_010", "재시도 횟수를 초과했습니다 (최대 3회)");
```

#### 3-2. RecommendationException.java

```java
// 추가할 팩토리 메서드
public static RecommendationException insufficientTokens();
public static RecommendationException retryLimitExceeded(Long sessionId);
```

---

### Step 4: Service 수정

#### 4-1. RecommendationService.java - generateRecommendation 메서드 변경

**변경 전:**
```java
// 5. 중복 생성 방지
if (recommendationRepository.existsBySessionId(sessionId)) {
    throw RecommendationException.recommendationAlreadyExists(sessionId);
}
```

**변경 후:**
```java
// 5. 토큰 확인
if (!currentUser.hasTokens()) {
    throw RecommendationException.insufficientTokens();
}

// 6. 재시도 횟수 확인 (최대 3회)
long attemptCount = recommendationRepository.countBySessionId(sessionId);
if (attemptCount >= 3) {
    throw RecommendationException.retryLimitExceeded(sessionId);
}

// 7. 토큰 차감
currentUser.useToken(1);

// ... AI 추천 요청 ...

// 8. Entity 변환 (attemptNumber 포함)
int attemptNumber = (int) attemptCount + 1;
```

#### 4-2. convertToEntities 메서드 시그니처 변경

**변경 전:**
```java
private List<Recommendation> convertToEntities(QuizSession session, RecommendationResponse response)
```

**변경 후:**
```java
private List<Recommendation> convertToEntities(QuizSession session, RecommendationResponse response, int attemptNumber)
```

---

### Step 5: Response DTO 수정

#### 5-1. RecommendationSummaryResponse.java

```java
// 추가할 필드
@Schema(description = "재시도 번호 (1~3)", example = "1")
Integer attemptNumber
```

#### 5-2. RecommendationMetaResponse.java (신규)

```java
@Schema(description = "추천 메타 정보")
public record RecommendationMetaResponse(
    @Schema(description = "남은 토큰", example = "2")
    Integer remainingTokens,

    @Schema(description = "남은 재시도 횟수", example = "2")
    Integer remainingRetries,

    @Schema(description = "최대 재시도 횟수", example = "3")
    Integer maxRetries
) {}
```

#### 5-3. GenerateRecommendationResponse.java (신규 - Wrapper)

```java
@Schema(description = "추천 생성 응답")
public record GenerateRecommendationResponse(
    List<RecommendationSummaryResponse> recommendations,
    RecommendationMetaResponse meta
) {}
```

---

### Step 6: Controller 수정

#### 6-1. RecommendationController.java - generateRecommendation 응답 변경

**변경 전:**
```java
return ApiResponse.success(
    recommendations.stream()
        .map(RecommendationSummaryResponse::from)
        .toList()
);
```

**변경 후:**
```java
// 메타 정보 생성
RecommendationMetaResponse meta = new RecommendationMetaResponse(
    currentUser.getRecommendationTokens(),
    3 - recommendations.get(0).getAttemptNumber(),
    3
);

GenerateRecommendationResponse response = new GenerateRecommendationResponse(
    recommendations.stream()
        .map(RecommendationSummaryResponse::from)
        .toList(),
    meta
);

return ApiResponse.success(response);
```

#### 6-2. Swagger 문서 업데이트

- 429 에러 응답 추가
- 402 에러 응답 추가
- 응답 예시 업데이트

---

### Step 7: 회원가입 시 토큰 지급

#### 7-1. CustomOAuth2UserService.java

```java
// createNewUser 메서드에서
User newUser = User.builder()
    // 기존 필드...
    .recommendationTokens(3)  // 가입 보너스
    .build();
```

---

### Step 8: DB 마이그레이션

```sql
-- users 테이블
ALTER TABLE users
ADD COLUMN recommendation_tokens INTEGER NOT NULL DEFAULT 0;

-- 기존 사용자에게 3토큰 지급
UPDATE users SET recommendation_tokens = 3;

-- recommendations 테이블
ALTER TABLE recommendations
ADD COLUMN attempt_number INTEGER NOT NULL DEFAULT 1;
```

---

### Step 9: 테스트 코드 수정

#### 9-1. RecommendationServiceTest.java

- 기존 "실패 - 이미 추천 존재" 테스트 제거
- 새 테스트 추가:
  - "성공 - 재시도 (attemptNumber 증가)"
  - "실패 - 토큰 부족"
  - "실패 - 재시도 횟수 초과"

#### 9-2. RecommendationControllerTest.java

- 402, 429 응답 테스트 추가

---

## 파일 변경 목록

| 순서 | 파일 | 변경 유형 | 설명 |
|------|------|----------|------|
| 1 | `User.java` | 수정 | 토큰 필드/메서드 추가 |
| 2 | `Recommendation.java` | 수정 | attemptNumber 필드 추가 |
| 3 | `RecommendationRepository.java` | 수정 | countBySessionId 추가 |
| 4 | `RecommendationErrorCode.java` | 수정 | 2개 에러 코드 추가 |
| 5 | `RecommendationException.java` | 수정 | 2개 팩토리 메서드 추가 |
| 6 | `RecommendationService.java` | 수정 | 토큰/재시도 로직 |
| 7 | `RecommendationSummaryResponse.java` | 수정 | attemptNumber 필드 추가 |
| 8 | `RecommendationMetaResponse.java` | 신규 | 메타 정보 DTO |
| 9 | `GenerateRecommendationResponse.java` | 신규 | Wrapper DTO |
| 10 | `RecommendationController.java` | 수정 | 응답 구조 변경 |
| 11 | `CustomOAuth2UserService.java` | 수정 | 가입 시 토큰 지급 |
| 12 | DB Migration | 신규 | SQL 스크립트 |
| 13 | `RecommendationServiceTest.java` | 수정 | 테스트 케이스 변경 |
| 14 | `RecommendationControllerTest.java` | 수정 | 테스트 케이스 추가 |

---

## 구현 순서

```
1. Entity 수정 (User, Recommendation)
   ↓
2. Repository 수정
   ↓
3. ErrorCode/Exception 추가
   ↓
4. Service 로직 수정
   ↓
5. Response DTO 생성/수정
   ↓
6. Controller 수정
   ↓
7. 회원가입 로직 수정
   ↓
8. DB 마이그레이션
   ↓
9. 테스트 코드 수정
   ↓
10. 빌드 및 테스트
```

---

## 예상 응답

### 성공 (첫 번째 추천)

```json
{
  "success": true,
  "data": {
    "recommendations": [
      {
        "recommendationId": 1,
        "attemptNumber": 1,
        "occasion": "데이트",
        "style": "캐주얼",
        "items": [...]
      }
    ],
    "meta": {
      "remainingTokens": 2,
      "remainingRetries": 2,
      "maxRetries": 3
    }
  }
}
```

### 성공 (재시도)

```json
{
  "success": true,
  "data": {
    "recommendations": [
      {
        "recommendationId": 5,
        "attemptNumber": 2,
        "occasion": "출근",
        "style": "비즈니스 캐주얼",
        "items": [...]
      }
    ],
    "meta": {
      "remainingTokens": 1,
      "remainingRetries": 1,
      "maxRetries": 3
    }
  }
}
```

### 실패 - 토큰 부족 (402)

```json
{
  "success": false,
  "error": {
    "code": "REC_009",
    "message": "추천 토큰이 부족합니다"
  }
}
```

### 실패 - 재시도 초과 (429)

```json
{
  "success": false,
  "error": {
    "code": "REC_010",
    "message": "재시도 횟수를 초과했습니다 (최대 3회)"
  }
}
```

---

## 예상 소요 시간

| 단계 | 예상 작업량 |
|------|------------|
| Entity/Repository | 간단 |
| ErrorCode/Exception | 간단 |
| Service 로직 | 중간 |
| Response DTO | 중간 |
| Controller | 간단 |
| 회원가입 수정 | 간단 |
| DB 마이그레이션 | 간단 |
| 테스트 코드 | 중간 |

---

## 주의사항

1. **트랜잭션**: 토큰 차감과 추천 생성이 하나의 트랜잭션으로 처리되어야 함
2. **동시성**: 같은 세션에 동시 요청 시 race condition 주의
3. **롤백**: AI 호출 실패 시 토큰 차감 롤백 필요
4. **기존 데이터**: 기존 추천의 attemptNumber는 1로 설정
