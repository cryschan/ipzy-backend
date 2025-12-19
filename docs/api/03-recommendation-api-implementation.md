# AI 추천 API 구현 문서

> 작성일: 2025-12-11
> 버전: v1.0
> 상태: 구현 완료

## 개요

Python FastAPI와 통신하여 AI 코디 추천을 생성하는 기능의 구현 문서입니다.

---

## 1. 아키텍처

### 1.1 전체 구조

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Client (Frontend)                           │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    RecommendationController                         │
│  POST /api/recommendations/sessions/{id}/generate  (인증 필수)      │
│  GET  /api/recommendations/sessions/{id}           (인증 선택)      │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     RecommendationService                           │
│  - 세션 검증, 권한 검증                                              │
│  - 익명 세션 → 사용자 연결                                          │
│  - AI 추천 요청 및 저장                                             │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   AiRecommendationClient (Interface)                │
└──────────────┬──────────────────────────────────┬───────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────┐
│             PythonAiClient                       │
│          (기본 활성화)                            │
│        - 실제 HTTP 통신                          │
│        - 테스트 시 @MockBean 사용                │
└──────────────┬───────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────┐
│           Python FastAPI                         │
│         POST /api/recommend                      │
└──────────────────────────────────────────────────┘
```

### 1.2 클라이언트 구성

| 환경 | 클라이언트 | 설명 |
|------|-----------|------|
| 모든 환경 | PythonAiClient | Python FastAPI와 실제 HTTP 통신 |
| 테스트 환경 | @MockBean AiRecommendationClient | 단위 테스트 시 Mock 사용 |

**참고**: 이전 버전의 MockAiClient는 제거되었습니다. Python 서버 없이 개발하려면 Python Docker Compose를 실행하세요.

---

## 2. API 명세

### 2.1 통신 테스트

**GET** `/api/recommendations/test`

| 항목 | 값 |
|------|-----|
| 인증 | 불필요 |
| 용도 | Python AI 서비스 연결 테스트 |

**Request**
```
GET /api/recommendations/test?msg=안녕하세요
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": "[Mock] Python 응답: 안녕하세요"
}
```

---

### 2.2 요청 데이터 미리보기 (테스트용)

**GET** `/api/recommendations/sessions/{sessionId}/preview-request`

| 항목 | 값 |
|------|-----|
| 인증 | 선택 (비로그인 허용) |
| 용도 | Python API 개발 시 요청 형식 확인 |

**Request**
```
GET /api/recommendations/sessions/123/preview-request
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "sessionId": 123,
    "answers": [
      {
        "questionId": 1,
        "questionText": "선호하는 스타일은?",
        "selectedOptions": ["캐주얼", "스트릿"]
      }
    ]
  }
}
```

**Error Responses**

| 상태 코드 | 에러 코드 | 설명 |
|----------|----------|------|
| 400 | REC302 | 퀴즈 미완료 |
| 403 | REC401 | 접근 권한 없음 |
| 404 | REC301 | 세션 없음 |

---

### 2.3 코디 추천 생성

**POST** `/api/recommendations/sessions/{sessionId}/generate`

| 항목 | 값 |
|------|-----|
| 인증 | **필수** (로그인 필요) |
| 권한 | 세션 소유자 또는 익명 세션 |

**Request**
```
POST /api/recommendations/sessions/123/generate
Authorization: Cookie (JSESSIONID)
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": [
    {
      "recommendationId": 1,
      "displayOrder": 1,
      "occasion": "데이트",
      "season": "봄",
      "style": "캐주얼",
      "reason": "밝은 색감의 캐주얼 룩으로 데이트에 적합합니다.",
      "totalPrice": 237000,
      "styleBoardUrl": "https://example.com/style_board1.jpg",
      "items": [
        {
          "itemId": 1,
          "productId": 101,
          "category": "TOP",
          "displayOrder": 1,
          "productName": "오버핏 옥스포드 셔츠",
          "brand": "무신사 스탠다드",
          "price": 59000,
          "imageUrl": "https://example.com/image1.jpg",
          "linkUrl": "https://example.com/product1"
        }
      ]
    }
  ]
}
```

**Error Responses**

| 상태 코드 | 에러 코드 | 설명 |
|----------|----------|------|
| 401 | AUTH_001 | 비로그인 상태 |
| 401 | AUTH_005 | 세션 만료 |
| 403 | REC401 | 다른 사용자의 세션 접근 |
| 404 | REC301 | 세션 없음 |
| 400 | REC302 | 퀴즈 미완료 |
| 409 | REC303 | 이미 추천 생성됨 |

---

### 2.4 추천 조회

**GET** `/api/recommendations/sessions/{sessionId}`

| 항목 | 값 |
|------|-----|
| 인증 | 선택 (비로그인 허용) |
| 권한 | 익명 세션: 누구나 / 로그인 세션: 소유자만 |

---

## 3. 익명 사용자 처리 (세션 연결)

### 3.1 비즈니스 요구사항

- 퀴즈는 **비로그인**으로 풀 수 있음
- 추천을 받으려면 **로그인 필수**
- 비로그인으로 퀴즈를 풀고 로그인하면, 기존 세션을 사용자에게 연결

### 3.2 흐름도

```
┌─────────────────────────────────────────────────────────────┐
│  1. 비로그인 사용자가 퀴즈 풀기                              │
│     → 익명 QuizSession 생성 (user = null)                  │
│     → 프론트엔드에서 sessionId 저장 (localStorage)          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  2. 추천 요청 시 → 401 Unauthorized                         │
│     → 프론트엔드에서 로그인 페이지로 이동                    │
│     → sessionId는 계속 유지                                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  3. 로그인 완료 후 다시 추천 요청                            │
│     → 익명 세션을 현재 사용자에게 연결 (assignUser)          │
│     → 추천 생성 및 저장                                      │
└─────────────────────────────────────────────────────────────┘
```

### 3.3 Security 설정

```java
// SecurityConfig.java
.requestMatchers(HttpMethod.POST, "/api/recommendations/sessions/*/generate")
    .authenticated()  // 로그인 필수
.requestMatchers(HttpMethod.GET, "/api/recommendations/sessions/*")
    .permitAll()      // 비로그인 허용
```

### 3.4 서비스 로직

```java
// RecommendationService.java
public List<Recommendation> generateRecommendation(Long sessionId, Long currentUserId) {
    QuizSession session = findCompletedSession(sessionId);
    User currentUser = userRepository.findById(currentUserId)...;

    // 익명 세션이면 현재 사용자에게 연결
    if (session.isAnonymous()) {
        session.assignUser(currentUser);
    } else {
        // 다른 사용자의 세션이면 거부
        validateSessionOwnership(session, currentUserId);
    }

    // AI 추천 요청...
}
```

---

## 4. DTO 구조

### 4.1 Python → Spring 통신용 DTO

```
dto/request/
├── RecommendationRequest.java      # 요청 최상위
└── QuizAnswerDto.java              # 퀴즈 답변

dto/response/
├── RecommendationResponse.java     # 응답 최상위
├── OutfitRecommendationDto.java    # 코디 추천 (toEntity 포함)
└── RecommendedItemDto.java         # 추천 아이템 (toEntity 포함)
```

### 4.2 클라이언트 응답용 DTO

```
dto/response/
├── RecommendationSummaryResponse.java  # 추천 요약 (from 메서드)
└── RecommendationItemResponse.java     # 아이템 응답 (from 메서드)
```

### 4.3 JSON 예시

**Spring → Python 요청**
```json
{
  "sessionId": 123,
  "answers": [
    {
      "questionId": 1,
      "questionText": "선호하는 스타일은?",
      "selectedOptions": ["캐주얼", "스트릿"]
    }
  ]
}
```

**Python → Spring 응답**
```json
{
  "recommended_outfits": [
    {
      "displayOrder": 1,
      "occasion": "데이트",
      "season": "봄",
      "style": "캐주얼",
      "reason": "밝은 색감의 캐주얼 룩입니다.",
      "total_price": 237000,
      "style_board_url": "https://example.com/style1.jpg",
      "items": [
        {
          "productId": 101,
          "category": "TOP",
          "name": "오버핏 셔츠",
          "brand": "무신사 스탠다드",
          "price": 59000,
          "imageUrl": "https://example.com/img1.jpg",
          "linkUrl": "https://example.com/product1"
        }
      ]
    }
  ]
}
```

---

## 5. 엔티티 구조

### 5.1 Recommendation

```java
@Entity
@Table(name = "recommendations")
public class Recommendation {
    private Long id;
    private User user;              // nullable (익명 → 연결 가능)
    private QuizSession session;
    private Integer displayOrder;
    private Integer totalPrice;
    private String reason;
    private String occasion;
    private String season;
    private String style;
    private String styleBoardUrl;
    private List<RecommendationItem> items;  // Cascade
}
```

### 5.2 RecommendationItem (Snapshot 패턴)

```java
@Entity
@Table(name = "recommendation_items")
public class RecommendationItem {
    private Long id;
    private Recommendation recommendation;
    private Long productId;           // FK 대신 ID만 저장
    private ClothingCategory category;
    private Integer displayOrder;

    // Snapshot 필드 (추천 시점의 상품 정보 보존)
    private String productNameSnapshot;
    private String brandSnapshot;
    private Integer priceSnapshot;
    private String imageUrlSnapshot;
    private String linkUrlSnapshot;
}
```

---

## 6. 에러 코드

| 코드 | HTTP | 메시지 |
|------|------|--------|
| REC001 | 404 | 추천 정보를 찾을 수 없습니다 |
| REC101 | 503 | AI 서비스에 연결할 수 없습니다 |
| REC102 | 504 | AI 서비스 응답 시간이 초과되었습니다 |
| REC103 | 500 | AI 서비스 응답을 처리할 수 없습니다 |
| REC201 | 400 | 추천된 상품을 찾을 수 없습니다 |
| REC301 | 404 | 퀴즈 세션을 찾을 수 없습니다 |
| REC302 | 400 | 퀴즈가 완료되지 않았습니다 |
| REC303 | 409 | 이미 추천이 생성된 세션입니다 |
| REC401 | 403 | 해당 세션에 접근 권한이 없습니다 |
| REC501 | 500 | AI 서비스에서 추천 결과가 없습니다 |

---

## 7. 파일 구조

```
src/main/java/com/ipzy/domain/recommendation/
├── client/
│   ├── AiRecommendationClient.java      # 인터페이스
│   └── PythonAiClient.java              # Python API 통신 (기본 활성화)
├── controller/
│   └── RecommendationController.java
├── dto/
│   ├── request/
│   │   ├── RecommendationRequest.java
│   │   └── QuizAnswerDto.java
│   └── response/
│       ├── RecommendationResponse.java
│       ├── OutfitRecommendationDto.java
│       ├── RecommendedItemDto.java
│       ├── RecommendationSummaryResponse.java
│       └── RecommendationItemResponse.java
├── entity/
│   ├── Recommendation.java
│   └── RecommendationItem.java
├── exception/
│   ├── RecommendationErrorCode.java
│   └── RecommendationException.java
├── repository/
│   ├── RecommendationRepository.java
│   └── RecommendationItemRepository.java
└── service/
    └── RecommendationService.java

src/test/java/com/ipzy/domain/recommendation/
├── client/
│   └── PythonAiClientTest.java          # (선택적)
├── controller/
│   └── RecommendationControllerTest.java
├── dto/
│   ├── request/
│   │   ├── QuizAnswerDtoTest.java
│   │   └── RecommendationRequestTest.java
│   └── response/
│       ├── OutfitRecommendationDtoTest.java
│       └── RecommendationSummaryResponseTest.java
├── entity/
│   ├── RecommendationTest.java
│   └── RecommendationItemTest.java
└── service/
    └── RecommendationServiceTest.java
```

---

## 8. 설정

### 8.1 application.yml

```yaml
ai:
  python:
    base-url: ${AI_SERVICE_URL:http://localhost:8000}
    connect-timeout: 5000      # 5초
    read-timeout: 10000        # 10초
```

### 8.2 프로파일 실행 예시

```bash
# 로컬 개발 (Mock 사용)
./gradlew bootRun --args='--spring.profiles.active=local'

# 운영 환경 (실제 Python 통신)
./gradlew bootRun --args='--spring.profiles.active=prod'
```

---

## 9. 프론트엔드 연동 가이드

### 9.1 퀴즈 완료 후 sessionId 저장

```javascript
// 퀴즈 완료 시
const response = await completeQuiz(quizId);
const sessionId = response.data.sessionId;

// localStorage에 저장 (로그인 후에도 유지)
localStorage.setItem('pendingSessionId', sessionId);
```

### 9.2 추천 요청 및 401 처리

```javascript
try {
  const sessionId = localStorage.getItem('pendingSessionId');
  const response = await fetch(`/api/recommendations/sessions/${sessionId}/generate`, {
    method: 'POST',
    credentials: 'include'  // 쿠키 포함
  });

  if (response.ok) {
    const data = await response.json();
    // 추천 결과 표시
    localStorage.removeItem('pendingSessionId');
  }
} catch (error) {
  if (error.status === 401) {
    // 로그인 페이지로 이동 (sessionId는 유지)
    router.push('/login?redirect=/recommendations');
  }
}
```

### 9.3 로그인 후 자동 추천 요청

```javascript
// 로그인 성공 콜백
const pendingSessionId = localStorage.getItem('pendingSessionId');
if (pendingSessionId) {
  // 자동으로 추천 요청
  await generateRecommendation(pendingSessionId);
}
```

---

## 10. 향후 개선 사항

| 항목 | 우선순위 | 상태 |
|------|----------|------|
| Circuit Breaker (Resilience4j) | 중간 | 미구현 |
| WireMock 통합 테스트 | 낮음 | 미구현 |
| 추천 결과 캐싱 | 낮음 | 미구현 |
| 재추천 기능 | 낮음 | 미구현 |
