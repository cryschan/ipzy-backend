# 아키텍처 및 기술 결정

## 기술 스택

| 영역 | 기술 | 버전 | 비고 |
|------|------|------|------|
| Language | Java | 21 | LTS |
| Framework | Spring Boot | 3.2.0 | |
| Database | PostgreSQL | 15+ | JSONB, Array 활용 |
| ORM | Spring Data JPA | | Hibernate |
| 인증 | Spring Security OAuth2 Client | | 카카오 연동 |
| AI | Spring AI | | OpenAI 연동 |
| 문서화 | SpringDoc | | Swagger UI |
| 빌드 | Gradle | | |

## 인증 방식: OAuth2 + Session

### 선택 이유

| 고려사항 | 이메일 인증 | OAuth2 (카카오) | 결정 |
|---------|-----------|-----------------|------|
| 가입 허들 | 높음 | 낮음 (원클릭) | **OAuth2** |
| 비밀번호 관리 | 필요 | 불필요 | **OAuth2** |
| 사용자 이탈률 | 높음 | 낮음 | **OAuth2** |
| 구현 복잡도 | 낮음 | 중간 | 감수 |

### OAuth2 로드맵

```
[MVP] 카카오 OAuth2 + Session
  - 카카오 로그인만 지원
  - 서버 1대 운영
  - 재시작 시 세션 만료 (재로그인)

[Phase 2] 네이버/구글 추가
  - 멀티 프로바이더 지원
  - Redis 세션 (다중 서버)
```

### 인증 플로우

```
[프론트엔드]                    [백엔드]                      [카카오]
     │                            │                            │
     │  GET /api/auth/kakao       │                            │
     │ ─────────────────────────> │                            │
     │                            │  302 Redirect              │
     │ <───────────────────────── │ ─────────────────────────> │
     │                            │                            │
     │                      카카오 로그인 페이지                │
     │ <─────────────────────────────────────────────────────> │
     │                            │                            │
     │                            │  callback (code)           │
     │                            │ <───────────────────────── │
     │                            │                            │
     │                            │  토큰 교환 & 사용자 정보     │
     │                            │ ─────────────────────────> │
     │                            │ <───────────────────────── │
     │                            │                            │
     │  302 Redirect + JSESSIONID │  세션 생성 & 사용자 저장     │
     │ <───────────────────────── │                            │
```

### SecurityConfig 설정

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/quizzes/**", "/api/products/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(endpoint -> endpoint
                    .baseUri("/api/auth")
                )
                .redirectionEndpoint(endpoint -> endpoint
                    .baseUri("/api/auth/*/callback")
                )
                .userInfoEndpoint(endpoint -> endpoint
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2SuccessHandler)
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(5)
            )
            .build();
    }
}
```

### User 엔티티 변경

```java
@Entity
public class User {
    @Id @GeneratedValue
    private Long id;

    private String email;
    private String name;
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;  // KAKAO, NAVER, GOOGLE

    private String providerId;      // OAuth2 provider의 사용자 ID

    @Enumerated(EnumType.STRING)
    private Role role;

    // password 필드 제거
}

public enum AuthProvider {
    KAKAO, NAVER, GOOGLE
}
```

## 비회원 처리

비회원도 퀴즈 조회, 상품 조회는 가능하지만, **추천 저장은 로그인 필요**.

```
비회원 → 퀴즈/상품 조회만 가능
회원   → 퀴즈 → 추천 → 저장
```

## 디렉토리 구조

```
src/main/java/com/ipzy/
├── domain/
│   ├── auth/
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java
│   │   └── dto/
│   │       ├── request/
│   │       └── response/
│   ├── quiz/
│   │   ├── controller/QuizController.java
│   │   ├── service/QuizService.java
│   │   ├── repository/
│   │   └── dto/
│   ├── recommendation/
│   │   ├── controller/RecommendationController.java
│   │   ├── service/RecommendationService.java
│   │   ├── repository/
│   │   └── dto/
│   ├── product/
│   │   ├── controller/ProductController.java
│   │   ├── service/ProductService.java
│   │   ├── repository/
│   │   └── dto/
│   ├── outfit/
│   │   ├── controller/OutfitController.java
│   │   ├── service/SavedOutfitService.java
│   │   ├── repository/
│   │   └── dto/
│   └── user/
│       └── (기존 유지)
├── global/
│   ├── ai/
│   │   └── service/AiRecommendationService.java
│   ├── config/
│   ├── common/
│   └── exception/
└── IpzyApplication.java
```

## 레이어별 책임

| 레이어 | 책임 | 규칙 |
|--------|------|------|
| Controller | 요청/응답 처리 | DTO만 사용, 비즈니스 로직 X |
| Service | 비즈니스 로직 | 트랜잭션 관리, Entity ↔ DTO 변환 |
| Repository | 데이터 접근 | JPA 쿼리, Entity만 반환 |
| DTO | 데이터 전송 | Request/Response 분리 |

## 응답 형식

### 성공 응답

```json
{
  "success": true,
  "data": { ... }
}
```

### 에러 응답

```json
{
  "success": false,
  "error": {
    "code": "AUTH_001",
    "message": "유효하지 않은 이메일 또는 비밀번호입니다"
  }
}
```

### 페이지네이션 응답

```json
{
  "success": true,
  "data": {
    "items": [...],
    "pagination": {
      "currentPage": 1,
      "totalPages": 10,
      "totalItems": 95,
      "itemsPerPage": 10,
      "hasNext": true,
      "hasPrev": false
    }
  }
}
```

---

## 응답 클래스 사용법

### ApiResponse - 기본 응답

```java
// 성공 응답
@GetMapping("/{id}")
public ApiResponse<ProductResponse> getProduct(@PathVariable Long id) {
    ProductResponse product = productService.getDetail(id);
    return ApiResponse.success(product);
}

// 에러 응답 (서비스에서 예외 던지면 GlobalExceptionHandler가 처리)
throw new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND);
```

### PageResponse - 페이지네이션

```java
@GetMapping
public ApiResponse<PageResponse<ProductResponse>> getProducts(Pageable pageable) {
    Page<ProductResponse> page = productService.findAll(pageable);
    return ApiResponse.success(PageResponse.from(page));
}
```

### 에러 코드 정의

```java
@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROD_001", "상품을 찾을 수 없습니다"),
    PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "PROD_002", "품절된 상품입니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

---

## 에러 코드 체계

프론트엔드와 통일된 에러 코드 체계를 사용합니다.

| 도메인 | Prefix | 예시 |
|--------|--------|------|
| 인증 | AUTH | AUTH_001, AUTH_002 |
| 사용자 | USER | USER_001, USER_002 |
| 퀴즈 | QUIZ | QUIZ_001, QUIZ_002 |
| 코디 | OUTFIT | OUTFIT_001, OUTFIT_002 |
| 상품 | PROD | PROD_001, PROD_002 |
| 구독 | SUB | SUB_001, SUB_002 |
| 결제 | PAY | PAY_001, PAY_002 |
| 검증 | VAL | VAL_001, VAL_002 |

---

## 예외 처리 구조

```
global/
├── common/
│   ├── ApiResponse.java        # 통합 응답 (success, data, error)
│   └── PageResponse.java       # 페이지네이션 래퍼
└── exception/
    ├── BusinessException.java  # 기본 비즈니스 예외
    ├── ErrorCode.java          # 에러 코드 인터페이스
    ├── CommonErrorCode.java    # 공통 에러 코드
    └── GlobalExceptionHandler.java

domain/{domain}/exception/
├── {Domain}Exception.java      # 도메인 예외
└── {Domain}ErrorCode.java      # 도메인 에러 코드
```
