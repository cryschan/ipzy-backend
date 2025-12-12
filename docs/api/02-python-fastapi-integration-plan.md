# Python FastAPI 통신 설계 계획서 (v3)

## 변경 이력
| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| v1 | - | 초기 설계 |
| v2 | - | Mapper 제거, DTO 패턴 적용 |
| v3 | 2025-12 | Product FK 제거, Snapshot 패턴 적용, Python 응답 형식 확장 |

## 작업 요청
> "퀴즈 답변과 무신사가 크롤링한 정보를 외부 파이썬 FastAPI와 통신하는 방법을 TDD로 설계하고 보고한다."

---

## 1. 요구사항 분석

### 1.1 핵심 목표
- QuizSession 완료 시 **퀴즈 답변 데이터**를 Python FastAPI로 전송
- **상품 정보는 Python FastAPI에서 직접 DB 조회** (공유 PostgreSQL)
- Python AI 서비스로부터 **코디 추천 결과**를 받아 저장
- **TDD 방식**으로 개발하여 안정성 확보

### 1.1.1 공유 PostgreSQL 고려사항

> Python이 동일 DB에 직접 접근하므로 아래 사항 필수 검토

| 항목 | 정책 | 비고 |
|------|------|------|
| **DB 계정 분리** | `ipzy_spring` / `ipzy_python` 별도 계정 | 권한 분리, 감사 추적 |
| **Python 권한** | `SELECT` only on `products` | 읽기 전용, 쓰기 금지 |
| **마이그레이션 책임** | **Spring (Flyway) 단일화** | Python은 스키마 변경 금지 |
| **읽기 replica** | 부하 분산 시 Python은 replica 사용 | 장애 격리 |
| **인덱스 관리** | Python 쿼리 패턴 분석 후 인덱스 추가 | 슬로우쿼리 모니터링 |

```sql
-- Python 전용 계정 생성 예시
CREATE USER ipzy_python WITH PASSWORD '...';
GRANT SELECT ON products TO ipzy_python;
GRANT SELECT ON brands TO ipzy_python;
-- INSERT/UPDATE/DELETE 권한 없음
```

### 1.2 현재 상태
| 항목 | 상태 | 파일 위치 |
|------|------|-----------|
| PythonAiClient | Mock 응답만 구현 | `recommendation/client/PythonAiClient.java` |
| QuizAnswer | JSONB로 다중선택 저장 | `quiz/entity/QuizAnswer.java` |
| Product | 크롤링 데이터 저장 완료 | `product/entity/Product.java` |
| Recommendation | 엔티티 구조 완성 | `recommendation/entity/Recommendation.java` |

### 1.3 성공 기준
- [x] 퀴즈 답변 → Python API 요청 DTO 변환 테스트 통과 ✅
- [x] Python API 응답 → Recommendation 엔티티 변환 테스트 통과 ✅
- [x] 통합 테스트에서 전체 흐름 검증 ✅ (빌드 성공)

### 1.4 아키텍처 개요

```text
┌─────────────┐                     ┌─────────────┐
│   Spring    │                     │   Python    │
│   Boot      │  RecommendationReq  │   FastAPI   │
│             │  (퀴즈 답변만)        │             │
│             │────────────────────►│             │
│             │                     │  DB 조회    │
│             │                     │  (상품)     │
│             │                     │     ↓       │
│             │                     │  AI 추천    │
│             │  RecommendationRes  │             │
│             │◄────────────────────│             │
└──────┬──────┘                     └──────┬──────┘
       │                                   │
       │                                   │
       ▼                                   ▼
   ┌───────────────────────────────────────────┐
   │              PostgreSQL (공유)             │
   │  products, quiz_sessions, quiz_answers,   │
   │  recommendations, recommendation_items    │
   └───────────────────────────────────────────┘
```

### 1.5 데이터 흐름

```text
[1] 퀴즈 완료
        │
        ▼
[2] Spring: RecommendationRequest 생성
        │   ├── sessionId
        │   └── answers (List<QuizAnswerDto>)
        │
        ▼
[3] HTTP POST → Python FastAPI
        │
        ▼
[4] Python: DB에서 상품 조회 (products 테이블)
        │
        ▼
[5] Python: AI 추천 로직 수행
        │
        ▼
[6] Python: RecommendationResponse 반환
        │   └── recommendations (List<OutfitRecommendationDto>)
        │
        ▼
[7] Spring: Recommendation 엔티티 저장
        │
        ▼
[8] Spring: RecommendationSummaryResponse 반환 (→ Client)
```

---

## 2. 프로젝트 코드 패턴 분석

### 2.1 기존 패턴 (user/auth 도메인 참고)

```text
Controller → Service → Repository
    ↓           ↓
   DTO       Entity
```

**핵심 규칙:**
1. **Mapper 클래스 없음** - 별도의 Mapper 클래스를 사용하지 않음
2. **DTO 내부 `static from()` 메서드** - Entity → DTO 변환은 DTO 내부에서 처리
3. **Service는 Entity 반환** - Service 메서드는 Entity를 반환
4. **Controller에서 DTO 변환** - `DTO.from(entity)` 호출

**예시 (UserProfileResponse.java):**
```java
public record UserProfileResponse(...) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            ...
        );
    }
}
```

**테스트 패턴 (UserServiceTest.java):**
- `@ExtendWith(MockitoExtension.class)`
- `@Mock`, `@InjectMocks`
- `@Nested`, `@DisplayName`
- BDDMockito: `given(...).willReturn(...)`

---

## 3. 접근 방식 및 트레이드오프 분석

### 접근 방식 A: 동기식 RestClient 통신

**설명**: 현재 `PythonAiClient`의 RestClient를 확장하여 동기 방식으로 Python API 호출

**장점**:
- 기존 코드 패턴과 일관성 유지
- 구현 단순, 디버깅 용이
- 트랜잭션 관리 명확 (요청-응답 완료 후 저장)

**단점**:
- 블로킹 방식으로 응답 대기 시간만큼 스레드 점유
- Python AI 처리 시간이 길 경우 UX 저하 가능

**위험도**: Low

---

### 접근 방식 B: 비동기 WebClient + 이벤트 기반

**설명**: WebClient로 비동기 호출 후, ApplicationEvent로 결과 처리

**장점**:
- 논블로킹으로 서버 리소스 효율적 사용
- 스케일링에 유리

**단점**:
- 코드 복잡도 증가
- 트랜잭션 경계 관리 어려움
- WebFlux 의존성 추가 필요
- 테스트 작성 복잡

**위험도**: Medium

---

### 접근 방식 C: 동기 + Circuit Breaker

**설명**: RestClient 동기 방식 + Resilience4j Circuit Breaker 패턴 적용

**장점**:
- 기존 패턴 유지하면서 장애 대응력 강화
- Python 서비스 장애 시 빠른 실패 (fail-fast)

**단점**:
- Resilience4j 의존성 추가 필요
- Circuit Breaker 설정 튜닝 필요

**위험도**: Low-Medium

---

### 권장 접근 방식: **A안 (동기식 RestClient)**

**선택 이유**:
1. MVP 단계에서 복잡도를 낮추는 것이 우선
2. 기존 `PythonAiClient` 패턴 재활용 가능
3. TDD로 검증하기 용이
4. 추후 성능 이슈 발생 시 C안으로 확장 용이

**트레이드오프 요약**:
- 포기하는 것: 비동기 처리의 성능 이점
- 얻는 것: 단순성, 빠른 구현, 테스트 용이성

---

## 4. API 스펙 설계

### 4.1 Python FastAPI 요청 DTO

> **Note**:
> - 사용자 선호 DTO(`UserPreferenceDto`)는 차후에 고려합니다.
> - **상품 정보는 Python FastAPI에서 직접 DB 조회**하므로 전송하지 않습니다.

```java
// 추천 요청 (request 패키지)
public record RecommendationRequest(
    Long sessionId,
    List<QuizAnswerDto> answers
) {
    public static RecommendationRequest of(QuizSession session) {
        return new RecommendationRequest(
            session.getId(),
            session.getAnswers().stream()
                .map(QuizAnswerDto::from)
                .toList()
        );
    }
}

// 퀴즈 답변 DTO
public record QuizAnswerDto(
    Long questionId,
    String questionText,
    List<String> selectedOptions
) {
    public static QuizAnswerDto from(QuizAnswer answer) {
        return new QuizAnswerDto(
            answer.getQuestion().getId(),
            answer.getQuestion().getText(),
            answer.getSelectedOptions()
        );
    }
}
```

#### 차후 확장 예정
```java
// TODO: 사용자 선호 DTO (추후 구현)
// public record UserPreferenceDto(...)
```

#### 제외된 DTO
```java
// ProductSummaryDto - Python FastAPI에서 직접 DB 조회하므로 불필요
```

### 4.2 Python FastAPI 응답 DTO

> **v3 변경**: Product FK 제거, Python 응답에서 상품 정보(name, brand, price 등) 직접 포함

```java
// 추천 응답 (response 패키지)
public record RecommendationResponse(
    @JsonProperty("recommended_outfits")
    List<OutfitRecommendationDto> recommendedOutfits
) {}

// 개별 코디 추천
public record OutfitRecommendationDto(
    Integer displayOrder,
    String occasion,
    String season,
    String style,
    String reason,
    @JsonProperty("total_price") Integer totalPrice,       // v3 추가
    @JsonProperty("style_board_url") String styleBoardUrl, // v3 추가
    List<RecommendedItemDto> items
) {
    // Recommendation Entity로 변환
    public Recommendation toEntity(QuizSession session, User user) {
        return Recommendation.builder()
            .session(session)
            .user(user)
            .displayOrder(displayOrder)
            .occasion(occasion)
            .season(season)
            .style(style)
            .reason(reason)
            .totalPrice(totalPrice != null ? totalPrice : 0)
            .styleBoardUrl(styleBoardUrl)
            .build();
    }
}

// 추천 아이템 (v3: 상품 정보 포함)
public record RecommendedItemDto(
    Long productId,
    String category,
    String name,      // v3 추가: 상품명
    String brand,     // v3 추가: 브랜드
    Integer price,    // v3 추가: 가격
    String imageUrl,  // v3 추가: 이미지 URL
    String linkUrl    // v3 추가: 상품 링크
) {}
```

### 4.3 클라이언트 응답 DTO (v3 추가)

```java
// 추천 요약 응답 - 클라이언트용
public record RecommendationSummaryResponse(
    Long recommendationId,
    Integer displayOrder,
    String occasion,
    String season,
    String style,
    String reason,
    Integer totalPrice,
    String styleBoardUrl,
    List<RecommendationItemResponse> items
) {}

// 추천 아이템 응답 - 클라이언트용
public record RecommendationItemResponse(
    Long itemId,
    Long productId,
    String category,
    Integer displayOrder,
    String productName,
    String brand,
    Integer price,
    String imageUrl,
    String linkUrl
) {}
```

### 4.4 API 엔드포인트 (Python FastAPI)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/recommend` | 코디 추천 요청 |
| GET | `/api/v1/health` | 헬스 체크 |

---

## 5. 상세 구현 계획 (TDD)

### 5.1 영향받는 파일

#### 신규 생성 ✅
| 파일 | 설명 | 상태 |
|------|------|------|
| `recommendation/dto/request/RecommendationRequest.java` | Python API 요청 DTO | ✅ 완료 |
| `recommendation/dto/request/QuizAnswerDto.java` | 퀴즈 답변 DTO | ✅ 완료 |
| `recommendation/dto/response/RecommendationResponse.java` | Python API 응답 DTO | ✅ 완료 |
| `recommendation/dto/response/OutfitRecommendationDto.java` | 코디 추천 DTO | ✅ 완료 |
| `recommendation/dto/response/RecommendedItemDto.java` | 추천 아이템 DTO | ✅ 완료 |
| `recommendation/dto/response/RecommendationSummaryResponse.java` | 클라이언트 응답 DTO | ✅ 완료 |
| `recommendation/dto/response/RecommendationItemResponse.java` | 클라이언트 아이템 응답 DTO (v3 추가) | ✅ 완료 |
| `recommendation/service/RecommendationService.java` | 추천 비즈니스 로직 | ✅ 완료 |
| `recommendation/repository/RecommendationRepository.java` | 추천 저장소 | ✅ 완료 |

> **차후 추가 예정**: `UserPreferenceDto.java` (사용자 선호 DTO)
> **제외**: `ProductSummaryDto.java` (Python에서 직접 DB 조회)
> **참고**: `RecommendationItemRepository.java` - 기본 CRUD만 제공 (Cascade로 대부분 처리)

#### 수정 ✅
| 파일 | 변경 내용 | 상태 |
|------|-----------|------|
| `recommendation/client/PythonAiClient.java` | `requestRecommendation()` 메서드 추가 | ✅ 완료 |
| `recommendation/controller/RecommendationController.java` | 추천 요청 엔드포인트 추가 | ✅ 완료 |
| `recommendation/entity/Recommendation.java` | `styleBoardUrl` 필드 추가 (v3) | ✅ 완료 |
| `recommendation/entity/RecommendationItem.java` | Product FK 제거, snapshot 필드 추가 (v3) | ✅ 완료 |

#### 테스트 ✅
| 파일 | 설명 | 상태 |
|------|------|------|
| `recommendation/dto/request/RecommendationRequestTest.java` | 요청 DTO 변환 테스트 | ✅ 완료 |
| `recommendation/dto/response/OutfitRecommendationDtoTest.java` | 응답 DTO 변환 테스트 | ✅ 완료 |
| `recommendation/dto/response/RecommendationSummaryResponseTest.java` | 요약 응답 테스트 (v3 추가) | ✅ 완료 |
| `recommendation/client/PythonAiClientTest.java` | 클라이언트 테스트 확장 | ✅ 완료 |
| `recommendation/service/RecommendationServiceTest.java` | 서비스 단위 테스트 | ✅ 완료 |

---

### 5.2 단계별 작업 순서 (TDD Red-Green-Refactor)

#### 1단계: DTO 테스트 및 구현

**1-1. 요청 DTO 테스트 (Red)**
```java
@DisplayName("RecommendationRequest 생성")
class RecommendationRequestTest {

    @Test
    @DisplayName("QuizSession과 Products로 RecommendationRequest 생성")
    void of_success() {
        // given
        QuizSession session = createMockSession();
        List<Product> products = createMockProducts();

        // when
        RecommendationRequest request = RecommendationRequest.of(session, products, null);

        // then
        assertThat(request.sessionId()).isEqualTo(session.getId());
        assertThat(request.answers()).hasSize(session.getAnswers().size());
        assertThat(request.products()).hasSize(products.size());
    }
}
```

**1-2. DTO 구현 (Green)**
- `RecommendationRequest`, `QuizAnswerDto`, `ProductSummaryDto` 구현
- 각 DTO에 `static from()` 또는 `static of()` 메서드 포함

**1-3. 응답 DTO 테스트 및 구현**
- `OutfitRecommendationDto.toEntity()` 테스트
- Entity 변환 로직 구현

---

#### 2단계: PythonAiClient 확장

**2-1. 클라이언트 테스트 (Red)**
```java
@Nested
@DisplayName("requestRecommendation")
class RequestRecommendation {

    @Test
    @DisplayName("성공 - 추천 응답 반환")
    void success() {
        // given
        RecommendationRequest request = createMockRequest();
        // Mock RestClient 응답 설정

        // when
        RecommendationResponse response = pythonAiClient.requestRecommendation(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.recommendations()).isNotEmpty();
    }

    @Test
    @DisplayName("실패 - 타임아웃")
    void fail_timeout() { ... }

    @Test
    @DisplayName("실패 - 서버 에러")
    void fail_serverError() { ... }
}
```

**2-2. 클라이언트 구현 (Green)**
```java
public RecommendationResponse requestRecommendation(RecommendationRequest request) {
    log.info("Python AI 추천 요청 시작: sessionId={}", request.sessionId());

    RecommendationResponse response = pythonAiRestClient.post()
        .uri("/api/v1/recommend")
        .body(request)
        .retrieve()
        .body(RecommendationResponse.class);

    log.info("Python AI 추천 응답 완료: {} 개 코디",
        response.recommendations().size());

    return response;
}
```

---

#### 3단계: RecommendationService 구현

**3-1. 서비스 테스트 (Red)**
```java
@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock QuizSessionRepository quizSessionRepository;
    @Mock ProductRepository productRepository;
    @Mock RecommendationRepository recommendationRepository;
    @Mock PythonAiClient pythonAiClient;

    @InjectMocks RecommendationService recommendationService;

    @Nested
    @DisplayName("generateRecommendation")
    class GenerateRecommendation {

        @Test
        @DisplayName("성공 - 추천 생성 및 저장")
        void success() {
            // given
            Long sessionId = 1L;
            QuizSession session = createCompletedSession();
            List<Product> products = createActiveProducts();
            RecommendationResponse aiResponse = createMockAiResponse();

            given(quizSessionRepository.findById(sessionId))
                .willReturn(Optional.of(session));
            given(productRepository.findByIsActiveTrue())
                .willReturn(products);
            given(pythonAiClient.requestRecommendation(any()))
                .willReturn(aiResponse);

            // when
            List<Recommendation> result = recommendationService
                .generateRecommendation(sessionId);

            // then
            assertThat(result).hasSize(aiResponse.recommendations().size());
            verify(recommendationRepository).saveAll(any());
        }

        @Test
        @DisplayName("실패 - 세션 미완료")
        void fail_sessionNotCompleted() { ... }

        @Test
        @DisplayName("실패 - 세션 없음")
        void fail_sessionNotFound() { ... }
    }
}
```

**3-2. 서비스 구현 (Green)**
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final QuizSessionRepository quizSessionRepository;
    private final ProductRepository productRepository;
    private final RecommendationRepository recommendationRepository;
    private final PythonAiClient pythonAiClient;

    @Transactional
    public List<Recommendation> generateRecommendation(Long sessionId) {
        // 1. 세션 조회 및 검증
        QuizSession session = findCompletedSession(sessionId);

        // 2. Python AI 요청 (퀴즈 답변만 전송, 상품은 Python에서 직접 DB 조회)
        RecommendationRequest request = RecommendationRequest.of(session);
        RecommendationResponse response = pythonAiClient.requestRecommendation(request);

        // 3. Entity 변환 및 저장
        List<Recommendation> recommendations = convertAndSave(session, response);

        return recommendations;
    }

    private QuizSession findCompletedSession(Long sessionId) {
        QuizSession session = quizSessionRepository.findById(sessionId)
            .orElseThrow(() -> RecommendationException.sessionNotFound(sessionId));

        if (!session.getCompleted()) {
            throw RecommendationException.sessionNotCompleted(sessionId);
        }
        return session;
    }
}
```

---

#### 4단계: Controller 및 통합 테스트

**4-1. Controller 구현**
```java
@Operation(summary = "코디 추천 생성", description = "완료된 퀴즈 세션 기반으로 코디를 추천합니다.")
@PostMapping("/sessions/{sessionId}/generate")
public ApiResponse<List<RecommendationSummaryResponse>> generateRecommendation(
        @PathVariable Long sessionId) {

    List<Recommendation> recommendations = recommendationService
        .generateRecommendation(sessionId);

    return ApiResponse.success(
        recommendations.stream()
            .map(RecommendationSummaryResponse::from)
            .toList()
    );
}
```

**4-2. 통합 테스트**
- @SpringBootTest + WireMock으로 Python API Mock
- 전체 흐름 검증

---

### 5.3 테스트 계획

| 테스트 유형 | 범위 | 도구 |
|------------|------|------|
| 단위 테스트 | DTO `from()`, Service 로직 | JUnit 5, Mockito |
| 통합 테스트 | Controller + DB | @SpringBootTest, H2 |
| Mock 서버 테스트 | Python API 호출 | WireMock |

---

## 6. 위험 요소 및 대응 방안

| 위험 | 영향도 | 대응 방안 |
|------|--------|-----------|
| Python 서비스 응답 지연 | Medium | 타임아웃 설정 (기본 30초) + 로딩 UI |
| Python 서비스 장애 | High | 헬스체크 API로 사전 확인 + 에러 메시지 |
| 네트워크 오류 | Medium | 재시도 로직 (최대 2회) |
| 응답 데이터 불일치 | Low | DTO 검증 + 예외 처리 |

---

## 7. 예상 산출물 트리

```
src/main/java/com/ipzy/domain/recommendation/
├── client/
│   └── PythonAiClient.java              # 수정
├── controller/
│   └── RecommendationController.java    # 수정
├── dto/
│   ├── request/
│   │   ├── RecommendationRequest.java   # 신규 (static of 메서드)
│   │   └── QuizAnswerDto.java           # 신규 (static from 메서드)
│   └── response/
│       ├── RecommendationResponse.java       # 신규
│       ├── OutfitRecommendationDto.java      # 신규 (toEntity 메서드)
│       ├── RecommendedItemDto.java           # 신규
│       └── RecommendationSummaryResponse.java # 신규 (static from 메서드)
├── repository/
│   ├── RecommendationRepository.java    # 신규
│   └── RecommendationItemRepository.java # 신규 (기본 CRUD만)
└── service/
    └── RecommendationService.java       # 신규

# 차후 추가 예정: dto/request/UserPreferenceDto.java
# 제외: dto/request/ProductSummaryDto.java (Python에서 직접 DB 조회)

src/test/java/com/ipzy/domain/recommendation/
├── client/
│   └── PythonAiClientTest.java          # 확장
├── dto/
│   ├── request/
│   │   └── RecommendationRequestTest.java    # 신규
│   └── response/
│       └── OutfitRecommendationDtoTest.java  # 신규
├── service/
│   └── RecommendationServiceTest.java   # 신규
└── controller/
    └── RecommendationControllerTest.java # 신규
```

---

## 8. 코드 품질 체크리스트

- [ ] RestClient 타임아웃 설정 (⏳ 검토 필요)
- [x] API 응답 검증 (null 체크, 빈 배열 처리) ✅
- [x] 로깅 추가 (요청/응답/에러) ✅
- [x] 예외 처리 (`RecommendationException` 활용) ✅
- [x] DTO 내 `from()`/`of()` 메서드 일관성 ✅
- [x] 테스트 커버리지 - 빌드 성공 ✅
- [ ] imageUrl/linkUrl null 처리 정책 (⏳ 결정 필요)

---

## 9. 구현 완료 보고

### v3 변경사항 (2025-12)

**아키텍처 변경**:
- [x] Product FK 제거 → Snapshot 패턴 적용
- [x] Python 응답에서 상품 정보(name, brand, price, imageUrl, linkUrl) 직접 수신
- [x] `@JsonProperty`로 snake_case ↔ camelCase 매핑

**DTO 변경**:
- [x] `RecommendationResponse`: `recommendations` → `recommended_outfits`
- [x] `OutfitRecommendationDto`: `totalPrice`, `styleBoardUrl` 추가
- [x] `RecommendedItemDto`: `name`, `brand`, `price`, `imageUrl`, `linkUrl` 추가
- [x] `RecommendationItemResponse`: 신규 추가 (클라이언트 응답용)

**엔티티 변경**:
- [x] `Recommendation`: `styleBoardUrl` 필드 추가
- [x] `RecommendationItem`: Product FK 제거, snapshot 필드 6개 추가

---

### 최종 DTO 목록 (7개)

| 패키지 | 파일명 | 역할 |
|--------|--------|------|
| `dto/request/` | `RecommendationRequest.java` | Python 요청 최상위 |
| `dto/request/` | `QuizAnswerDto.java` | 퀴즈 답변 |
| `dto/response/` | `RecommendationResponse.java` | Python 응답 최상위 |
| `dto/response/` | `OutfitRecommendationDto.java` | 코디 추천 (Python) |
| `dto/response/` | `RecommendedItemDto.java` | 추천 아이템 (Python) |
| `dto/response/` | `RecommendationSummaryResponse.java` | 클라이언트 응답 |
| `dto/response/` | `RecommendationItemResponse.java` | 클라이언트 아이템 응답 (v3 추가) |

---

### 남은 작업

| 항목 | 우선순위 | 상태 |
|------|----------|------|
| RestClient 타임아웃 설정 | 높음 | ⏳ 검토 필요 |
| imageUrl/linkUrl null 처리 | 중간 | ⏳ 정책 결정 필요 |
| WireMock 통합 테스트 | 낮음 | 차후 |
| Circuit Breaker 적용 | 낮음 | 차후 |

---

### 요청/응답 JSON 예시

**Spring → Python 요청**:
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

**Python → Spring 응답**:
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

**✅ 구현 완료** - 빌드 및 테스트 통과
