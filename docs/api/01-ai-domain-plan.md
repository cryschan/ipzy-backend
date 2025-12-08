# AI 추천 시스템 통신 구조 설계

> 퀴즈 완료 후 Python AI 서비스로부터 코디 추천을 받아 저장하는 구조

## 1. 비즈니스 플로우

```
[사용자] → [퀴즈 완료] → [Java Backend] → [Python AI] → [추천 결과] → [DB 저장] → [화면 표시]
```

### 상세 플로우

1. **사용자가 퀴즈 완료** → `QuizSession.completed = true`
2. **자동 트리거**: 퀴즈 완료 시 추천 API 자동 호출 (프론트엔드 로딩 화면 표시)
3. **Java → Python 전송**: User 정보 + QuizSession (답변) + Product 목록
4. **Python AI 처리**: Product 목록에서 추천 코디 결정 + S3에서 코디 이미지 URL 조회
5. **Python → Java 응답**: 추천 코디 목록 + S3 이미지 URL
6. **Java DB 저장**: `Recommendation` + `RecommendationItem` 저장
7. **화면에 결과 표시**

### 성능 목표

| 항목 | 목표 |
|------|------|
| 응답 시간 | **5초 이내** |
| 최대 허용 | 10초 (타임아웃) |
| 프론트엔드 | 로딩 화면 표시 |

---

## 2. 엔티티 구조

### QuizSession (기존)
```java
- id, user, quiz, completed, answers[], createdAt
```

### Recommendation (수정 필요 ⚠️)
```java
- id, user, session(QuizSession), displayOrder, totalPrice
- reason, occasion, season, style
- outfitImageUrl  // ✅ 신규 추가: 코디 전체 이미지 URL
- items[]
```

### RecommendationItem (기존)
```java
- id, recommendation, product, category, displayOrder
- priceSnapshot, productNameSnapshot, imageUrlSnapshot
```

---

## 3. Python 통신 API 설계

### 3.1 Java → Python 요청

**POST** `/ai/recommend`

```json
{
  "userId": 123,
  "sessionId": 456,
  "userPreferences": {
    "favoriteColors": ["black", "white"],
    "preferredStyles": ["casual", "minimal"]
  },
  "quizAnswers": [
    {
      "questionId": 1,
      "questionText": "선호하는 스타일은?",
      "selectedOption": "캐주얼"
    },
    {
      "questionId": 2,
      "questionText": "주로 입는 상황은?",
      "selectedOption": "출근/데일리"
    }
  ],
  "products": [
    {
      "id": 101,
      "name": "베이직 화이트 셔츠",
      "category": "TOP",
      "price": 45000,
      "imageUrl": "https://example.com/product/101.jpg",
      "brand": "ZARA",
      "tags": ["캐주얼", "오피스룩", "봄"]
    },
    {
      "id": 205,
      "name": "슬림핏 치노 팬츠",
      "category": "BOTTOM",
      "price": 59000,
      "imageUrl": "https://example.com/product/205.jpg",
      "brand": "UNIQLO",
      "tags": ["캐주얼", "데일리", "사계절"]
    }
    // ... 더 많은 상품
  ]
}
```

### 3.2 Python → Java 응답

```json
{
  "recommendations": [
    {
      "displayOrder": 1,
      "reason": "답변하신 캐주얼 스타일과 출근용으로 적합한 코디입니다",
      "occasion": "출근",
      "season": "봄/가을",
      "style": "캐주얼",
      "outfitImageUrl": "https://ipzy-bucket.s3.ap-northeast-2.amazonaws.com/outfits/outfit-001.jpg",
      "items": [
        {
          "category": "TOP",
          "productId": 101,
          "displayOrder": 1
        },
        {
          "category": "BOTTOM",
          "productId": 205,
          "displayOrder": 2
        },
        {
          "category": "SHOES",
          "productId": 312,
          "displayOrder": 3
        }
      ]
    },
    {
      "displayOrder": 2,
      "reason": "...",
      "items": [...]
    }
  ]
}
```

---

## 4. 디렉토리 구조

```
com.ipzy
├── domain
│   └── recommendation
│       ├── client/
│       │   └── PythonAiClient.java            # Python 통신 클라이언트
│       ├── controller/
│       │   └── RecommendationController.java  # AI 추천 엔드포인트
│       ├── service/
│       │   └── RecommendationService.java     # AI 추천 로직 (예정)
│       └── exception/
│           ├── RecommendationErrorCode.java   # 에러 코드
│           └── RecommendationException.java   # 커스텀 예외
│
└── _global
    └── infrastructure/
        └── client/
            └── PythonAiClientConfig.java      # RestClient 설정
```

---

## 5. 설정

### application.yml

```yaml
ai:
  python:
    base-url: ${AI_SERVICE_URL:http://localhost:8000}
    connect-timeout: 5000
    read-timeout: 10000  # 웹 이미지 수집 시간 고려
```

---

## 6. 핵심 클래스

### 6.1 RestClient 설정

```java
@Configuration
public class PythonAiClientConfig {

    @Bean
    public RestClient pythonAiRestClient(
            @Value("${ai.python.base-url}") String baseUrl,
            @Value("${ai.python.connect-timeout}") int connectTimeout,
            @Value("${ai.python.read-timeout}") int readTimeout) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}
```

### 6.2 Python 통신 클라이언트

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PythonAiClient {

    private final RestClient pythonAiRestClient;

    public AiRecommendResponse requestRecommendation(AiRecommendRequest request) {
        try {
            return pythonAiRestClient.post()
                .uri("/ai/recommend")
                .body(request)
                .retrieve()
                .body(AiRecommendResponse.class);
        } catch (RestClientException e) {
            log.error("Python AI 서비스 통신 실패: {}", e.getMessage());
            throw new AiServiceException(AiErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }
}
```

### 6.3 요청/응답 DTO

```java
// AiRecommendRequest.java
@Getter
@Builder
public class AiRecommendRequest {
    private Long userId;
    private Long sessionId;  // 로깅/추적용
    private Map<String, Object> userPreferences;
    private List<QuizAnswerDto> quizAnswers;
    private List<ProductDto> products;

    @Getter
    @Builder
    public static class QuizAnswerDto {
        private Long questionId;
        private String questionText;
        private String selectedOption;
    }

    @Getter
    @Builder
    public static class ProductDto {
        private Long id;
        private String name;
        private String category;
        private Integer price;
        private String imageUrl;
        private String brand;
        private List<String> tags;
    }
}

// AiRecommendResponse.java
@Getter
@NoArgsConstructor
public class AiRecommendResponse {
    private List<RecommendationDto> recommendations;

    @Getter
    @NoArgsConstructor
    public static class RecommendationDto {
        private Integer displayOrder;
        private String reason;
        private String occasion;
        private String season;
        private String style;
        private String outfitImageUrl;  // S3에 저장된 코디 이미지 URL
        private List<ItemDto> items;
    }

    @Getter
    @NoArgsConstructor
    public static class ItemDto {
        private String category;
        private Long productId;
        private Integer displayOrder;
    }
}
```

### 6.4 Service (추천 생성 로직)

```java
@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationService {

    private final PythonAiClient pythonAiClient;
    private final RecommendationRepository recommendationRepository;
    private final ProductRepository productRepository;
    private final QuizSessionRepository quizSessionRepository;

    public List<Recommendation> generateRecommendations(Long sessionId) {
        // 1. QuizSession 조회
        QuizSession session = quizSessionRepository.findByIdWithAnswers(sessionId)
            .orElseThrow(() -> new QuizException(QuizErrorCode.SESSION_NOT_FOUND));

        // 2. Python AI 요청 생성
        AiRecommendRequest request = buildAiRequest(session);

        // 3. Python AI 호출
        AiRecommendResponse response = pythonAiClient.requestRecommendation(request);

        // 4. 응답을 엔티티로 변환 및 저장
        List<Recommendation> recommendations = convertAndSave(session, response);

        return recommendations;
    }

    private AiRecommendRequest buildAiRequest(QuizSession session) {
        // 퀴즈 답변 변환
        List<AiRecommendRequest.QuizAnswerDto> answers = session.getAnswers().stream()
            .map(a -> AiRecommendRequest.QuizAnswerDto.builder()
                .questionId(a.getQuestion().getId())
                .questionText(a.getQuestion().getQuestionText())
                .selectedOption(a.getSelectedOption().getOptionText())
                .build())
            .toList();

        // ✅ 가용 상품 목록 조회
        List<AiRecommendRequest.ProductDto> products = productRepository.findAll().stream()
            .map(p -> AiRecommendRequest.ProductDto.builder()
                .id(p.getId())
                .name(p.getName())
                .category(p.getCategory().name())
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .brand(p.getBrand() != null ? p.getBrand().getName() : null)
                .tags(p.getTags())
                .build())
            .toList();

        return AiRecommendRequest.builder()
            .userId(session.getUser() != null ? session.getUser().getId() : null)
            .sessionId(session.getId())
            .userPreferences(session.getUser() != null ? session.getUser().getPreferences() : null)
            .quizAnswers(answers)
            .products(products)
            .build();
    }

    private List<Recommendation> convertAndSave(QuizSession session, AiRecommendResponse response) {
        return response.getRecommendations().stream()
            .map(dto -> {
                Recommendation rec = Recommendation.builder()
                    .user(session.getUser())
                    .session(session)
                    .displayOrder(dto.getDisplayOrder())
                    .reason(dto.getReason())
                    .occasion(dto.getOccasion())
                    .season(dto.getSeason())
                    .style(dto.getStyle())
                    .outfitImageUrl(dto.getOutfitImageUrl())  // ✅ 추가
                    .build();

                // 아이템 추가
                dto.getItems().forEach(itemDto -> {
                    Product product = productRepository.findById(itemDto.getProductId())
                        .orElseThrow(() -> new ProductException(ProductErrorCode.NOT_FOUND));

                    RecommendationItem item = RecommendationItem.builder()
                        .product(product)
                        .category(ClothingCategory.valueOf(itemDto.getCategory()))
                        .displayOrder(itemDto.getDisplayOrder())
                        .build();

                    rec.addItem(item);
                });

                return recommendationRepository.save(rec);
            })
            .toList();
    }
}
```

### 6.5 Controller

```java
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * 퀴즈 완료 후 AI 추천 생성 요청
     */
    @PostMapping("/generate")
    public ApiResponse<List<RecommendationResponse>> generateRecommendations(
            @RequestParam Long sessionId) {

        List<Recommendation> recommendations =
            recommendationService.generateRecommendations(sessionId);

        List<RecommendationResponse> response = recommendations.stream()
            .map(RecommendationResponse::from)
            .toList();

        return ApiResponse.success(response);
    }
}
```

---

## 7. 시퀀스 다이어그램

```
Client          Java Backend           Python AI              S3              Database
  |                  |                     |                   |                  |
  |  퀴즈 완료       |                     |                   |                  |
  |----------------->|                     |                   |                  |
  |                  |                     |                   |                  |
  |                  |  User + Session     |                   |                  |
  |                  |  + Products         |                   |                  |
  |                  |-------------------->|                   |                  |
  |                  |                     |                   |                  |
  |                  |                     | AI 추론           |                  |
  |                  |                     | (코디 조합 결정)   |                  |
  |                  |                     |                   |                  |
  |                  |                     | 이미지 URL 조회    |                  |
  |                  |                     |------------------>|                  |
  |                  |                     |    S3 URL 반환    |                  |
  |                  |                     |<------------------|                  |
  |                  |                     |                   |                  |
  |                  |  추천 결과          |                   |                  |
  |                  |  + S3 이미지 URL    |                   |                  |
  |                  |<--------------------|                   |                  |
  |                  |                     |                   |                  |
  |                  |  Recommendation 저장                    |                  |
  |                  |---------------------------------------------------->|
  |                  |                     |                   |                  |
  |  추천 결과 표시   |                     |                   |                  |
  |<-----------------|                     |                   |                  |
```

---

## 8. 결정 사항 및 고려사항

### ✅ 결정된 사항

| 항목 | 결정 |
|------|------|
| 추천 트리거 | 퀴즈 완료 시 자동 호출 |
| 코디 이미지 | Recommendation에 `outfitImageUrl` 필드 추가 |
| Product 전달 | Java가 요청 시 Product 목록 함께 전송 |
| 응답 목표 | 5초 이내, 최대 10초 |

### 추가 고려사항

#### S3 이미지 관리
- 코디 이미지는 S3에 저장됨
- Python이 S3에서 이미지 URL을 조회하여 반환
- S3 URL은 안정적이므로 별도 캐싱 불필요

#### Product 목록 최적화
- 전체 Product를 전송하면 요청 크기가 커질 수 있음
- 필요시 카테고리별 필터링 또는 페이징 적용 검토

---

## 9. 에러 코드

```java
@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI001", "AI 서비스에 연결할 수 없습니다"),
    AI_REQUEST_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI002", "AI 서비스 응답 시간 초과"),
    AI_INVALID_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "AI003", "AI 응답 처리 실패"),
    PRODUCT_NOT_FOUND_IN_RECOMMENDATION(HttpStatus.BAD_REQUEST, "AI004", "추천된 상품을 찾을 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```
