# AI 추천 기능 - 미구현 작업 목록

> 작성일: 2025-12-11
> 관련 문서: `docs/api/02-python-fastapi-integration-plan.md`, `docs/api/03-recommendation-api-implementation.md`

---

## 1. RestClient 타임아웃 설정

### 우선순위: 높음

### 현재 상태
- `PythonAiClient`에서 RestClient 사용 중
- 타임아웃 설정 미적용

### 필요 작업
```java
// application.yml
ai:
  python:
    base-url: ${AI_SERVICE_URL:http://localhost:8000}
    connect-timeout: 5000      # 5초
    read-timeout: 10000        # 10초
```

```java
// PythonAiClient.java
@Configuration
public class RestClientConfig {

    @Value("${ai.python.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${ai.python.read-timeout:10000}")
    private int readTimeout;

    @Bean
    public RestClient pythonAiRestClient() {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(clientHttpRequestFactory())
            .build();
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
```

### 참고
- 문서 설계: connect 5초, read 10초
- Python AI 처리 시간에 따라 조정 필요

---

## 2. imageUrl/linkUrl null 처리 정책

### 우선순위: 중간

### 현재 상태
- `RecommendedItemDto`에서 null 허용
- `RecommendationItem` 엔티티에서 nullable로 설정

### 결정 필요 사항

| 옵션 | 설명 | 장점 | 단점 |
|------|------|------|------|
| A. null 허용 (현재) | 그대로 null 저장 | 단순함 | 프론트에서 null 체크 필요 |
| B. 기본값 사용 | 플레이스홀더 URL 설정 | 일관성 | 실제 없는 이미지 표시 |
| C. 필터링 | null인 아이템 제외 | 깔끔한 데이터 | 추천 아이템 누락 가능 |

### 권장 방안
- **A안 유지** + 프론트엔드에서 기본 이미지 처리
- 또는 DTO 레벨에서 기본값 설정:

```java
public record RecommendationItemResponse(
    // ...
    String imageUrl,  // null이면 프론트에서 기본 이미지 표시
    String linkUrl    // null이면 클릭 비활성화
) {}
```

---

## 3. WireMock 통합 테스트

### 우선순위: 낮음

### 현재 상태
- 단위 테스트만 작성됨 (AiRecommendationClient @Mock 사용)
- PythonAiClient 기본 활성화

### 필요 작업
```java
@SpringBootTest
@AutoConfigureWireMock(port = 0)
class PythonAiClientIntegrationTest {

    @Test
    void requestRecommendation_success() {
        stubFor(post(urlEqualTo("/api/v1/recommend"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "recommended_outfits": [...]
                    }
                    """)));

        // 테스트 실행
    }

    @Test
    void requestRecommendation_timeout() {
        stubFor(post(urlEqualTo("/api/v1/recommend"))
            .willReturn(aResponse()
                .withFixedDelay(15000)));  // 타임아웃 테스트

        // RecommendationException.aiRequestTimeout() 검증
    }
}
```

### 의존성 추가 필요
```gradle
testImplementation 'org.springframework.cloud:spring-cloud-contract-wiremock'
```

---

## 4. Circuit Breaker 적용

### 우선순위: 낮음 (운영 안정화 후)

### 현재 상태
- 장애 대응 로직 없음
- Python 서비스 장애 시 요청 계속 시도

### 필요 작업

#### 의존성 추가
```gradle
implementation 'org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j'
```

#### 설정
```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      pythonAi:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30000
        permittedNumberOfCallsInHalfOpenState: 3
```

#### 적용
```java
@CircuitBreaker(name = "pythonAi", fallbackMethod = "fallbackRecommendation")
public RecommendationResponse requestRecommendation(RecommendationRequest request) {
    // 기존 로직
}

private RecommendationResponse fallbackRecommendation(RecommendationRequest request, Exception e) {
    log.error("Python AI 서비스 장애, fallback 실행: {}", e.getMessage());
    throw RecommendationException.aiServiceUnavailable();
}
```

---

## 5. 추가 고려 사항

### 5.1 재추천 기능
- 현재: 세션당 1회 추천 제한 (`REC303`)
- 향후: 재추천 허용 여부 결정 필요

### 5.2 추천 결과 캐싱
- 동일 세션에 대한 반복 조회 시 캐싱 고려
- Redis 또는 Caffeine 캐시 적용 가능

### 5.3 비동기 처리 (장기)
- 현재: 동기 방식 (A안)
- 향후: AI 응답 시간이 길어질 경우 비동기 전환 고려

---

## 체크리스트

- [ ] RestClient 타임아웃 설정
- [ ] imageUrl/linkUrl null 처리 정책 결정
- [ ] WireMock 통합 테스트 작성
- [ ] Circuit Breaker 적용
- [ ] 재추천 기능 정책 결정
- [ ] 캐싱 전략 수립
