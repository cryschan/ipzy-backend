# MVP 구현 체크리스트

## Phase 1: 핵심 플로우 (퀴즈 → 추천)

### 1.1 Repository

| 상태 | 항목 | 파일 경로 | 비고 |
|:----:|------|----------|------|
| [ ] | QuizRepository | `domain/quiz/repository/QuizRepository.java` | |
| [ ] | QuizQuestionRepository | `domain/quiz/repository/QuizQuestionRepository.java` | |
| [ ] | QuizSessionRepository | `domain/quiz/repository/QuizSessionRepository.java` | |
| [ ] | QuizAnswerRepository | `domain/quiz/repository/QuizAnswerRepository.java` | |
| [ ] | ProductRepository | `domain/product/repository/ProductRepository.java` | |
| [ ] | BrandRepository | `domain/product/repository/BrandRepository.java` | |
| [ ] | RecommendationRepository | `domain/recommendation/repository/RecommendationRepository.java` | |
| [ ] | RecommendationItemRepository | `domain/recommendation/repository/RecommendationItemRepository.java` | |

---

### 1.2 Service

| 상태 | 항목 | 파일 경로 | 주요 메서드 |
|:----:|------|----------|------------|
| [ ] | QuizService | `domain/quiz/service/QuizService.java` | getQuizDetail, startSession, submitAnswers |
| [ ] | AiRecommendationService | `domain/recommendation/service/AiRecommendationService.java` | generateRecommendations |
| [ ] | RecommendationService | `domain/recommendation/service/RecommendationService.java` | getDetail, getBySessionId |
| [ ] | ProductService | `domain/product/service/ProductService.java` | getDetail |
| [ ] | BrandService | `domain/product/service/BrandService.java` | getAll |

---

### 1.3 Controller

| 상태 | 항목 | 파일 경로 | 엔드포인트 |
|:----:|------|----------|-----------|
| [ ] | QuizController | `domain/quiz/controller/QuizController.java` | GET /{quizId}, POST /sessions, POST /submit, POST /recommend |
| [ ] | OutfitController | `domain/outfit/controller/OutfitController.java` | GET /{recommendationId} |
| [ ] | ProductController | `domain/product/controller/ProductController.java` | GET /{productId} |
| [ ] | BrandController | `domain/product/controller/BrandController.java` | GET / |

---

### 1.4 DTO - Request

| 상태 | 항목 | 파일 경로 |
|:----:|------|----------|
| [ ] | QuizSubmitRequest | `domain/quiz/dto/request/QuizSubmitRequest.java` |
| [ ] | AnswerRequest | `domain/quiz/dto/request/AnswerRequest.java` |

---

### 1.5 DTO - Response

| 상태 | 항목 | 파일 경로 |
|:----:|------|----------|
| [ ] | QuizDetailResponse | `domain/quiz/dto/response/QuizDetailResponse.java` |
| [ ] | QuestionResponse | `domain/quiz/dto/response/QuestionResponse.java` |
| [ ] | OptionResponse | `domain/quiz/dto/response/OptionResponse.java` |
| [ ] | QuizSessionResponse | `domain/quiz/dto/response/QuizSessionResponse.java` |
| [ ] | QuizSubmitResponse | `domain/quiz/dto/response/QuizSubmitResponse.java` |
| [ ] | RecommendationSummaryResponse | `domain/recommendation/dto/response/RecommendationSummaryResponse.java` |
| [ ] | RecommendationDetailResponse | `domain/recommendation/dto/response/RecommendationDetailResponse.java` |
| [ ] | RecommendationItemResponse | `domain/recommendation/dto/response/RecommendationItemResponse.java` |
| [ ] | ProductDetailResponse | `domain/product/dto/response/ProductDetailResponse.java` |
| [ ] | ProductSummaryResponse | `domain/product/dto/response/ProductSummaryResponse.java` |
| [ ] | BrandResponse | `domain/product/dto/response/BrandResponse.java` |

---

### 1.6 AI 연동

| 상태 | 항목 | 비고 |
|:----:|------|------|
| [ ] | build.gradle에 Spring AI 의존성 추가 | `spring-ai-openai-spring-boot-starter` |
| [ ] | application.yml AI 설정 | api-key, model, temperature |
| [ ] | AiRecommendationService 구현 | 프롬프트 생성, API 호출, 응답 파싱 |

---

### 1.7 초기 데이터 (Flyway)

| 상태 | 항목 | 파일 경로 |
|:----:|------|----------|
| [ ] | V2__insert_quiz_data.sql | `resources/db/migration/` |
| [ ] | V3__insert_brand_data.sql | `resources/db/migration/` |
| [ ] | V4__insert_product_data.sql | `resources/db/migration/` |

---

## Phase 2: 인증 + 저장 기능

### 2.1 OAuth2 인증

| 상태 | 항목 | 파일 경로 |
|:----:|------|----------|
| [ ] | build.gradle OAuth2 의존성 | `spring-boot-starter-oauth2-client` |
| [ ] | application.yml 카카오 설정 | client-id, client-secret, redirect-uri |
| [ ] | CustomOAuth2UserService | `domain/auth/service/CustomOAuth2UserService.java` |
| [ ] | OAuth2UserInfo | `domain/auth/dto/OAuth2UserInfo.java` |
| [ ] | KakaoOAuth2UserInfo | `domain/auth/dto/KakaoOAuth2UserInfo.java` |
| [ ] | CustomUserPrincipal | `domain/auth/dto/CustomUserPrincipal.java` |
| [ ] | OAuth2SuccessHandler | `domain/auth/handler/OAuth2SuccessHandler.java` |
| [ ] | OAuth2FailureHandler | `domain/auth/handler/OAuth2FailureHandler.java` |
| [ ] | SecurityConfig 수정 | OAuth2 로그인 설정 |

---

### 2.2 Auth Controller

| 상태 | 항목 | 엔드포인트 |
|:----:|------|-----------|
| [ ] | AuthController | `domain/auth/controller/AuthController.java` |
| [ ] | GET /api/auth/me | 현재 사용자 조회 |
| [ ] | POST /api/auth/logout | 로그아웃 |

---

### 2.3 저장된 코디

| 상태 | 항목 | 파일 경로 |
|:----:|------|----------|
| [ ] | SavedOutfitRepository | `domain/outfit/repository/SavedOutfitRepository.java` |
| [ ] | SavedOutfitService | `domain/outfit/service/SavedOutfitService.java` |
| [ ] | OutfitController 확장 | POST /save, GET /saved, DELETE /saved/{id} |
| [ ] | SaveOutfitRequest | `domain/outfit/dto/request/SaveOutfitRequest.java` |
| [ ] | SavedOutfitResponse | `domain/outfit/dto/response/SavedOutfitResponse.java` |

---

## Phase 3: 마무리

### 3.1 문서화

| 상태 | 항목 | 비고 |
|:----:|------|------|
| [ ] | SwaggerConfig | OpenAPI 3.0 설정 |
| [ ] | API 문서 어노테이션 | @Operation, @ApiResponse |

---

### 3.2 테스트

| 상태 | 항목 | 파일 경로 |
|:----:|------|----------|
| [ ] | QuizServiceTest | `test/domain/quiz/service/` |
| [ ] | RecommendationServiceTest | `test/domain/recommendation/service/` |
| [ ] | QuizFlowIntegrationTest | `test/integration/` |

---

### 3.3 에러 처리

| 상태 | 항목 | 비고 |
|:----:|------|------|
| [ ] | AI 응답 파싱 실패 처리 | 재시도 또는 기본 추천 |
| [ ] | 상품 품절 처리 | isActive 체크 |
| [ ] | 세션 타임아웃 처리 | 만료된 세션 처리 |

---

## 진행 현황

| Phase | 전체 | 완료 | 진행률 |
|-------|------|------|--------|
| Phase 1 | 35 | 0 | 0% |
| Phase 2 | 14 | 0 | 0% |
| Phase 3 | 6 | 0 | 0% |
| **합계** | **55** | **0** | **0%** |

---

## 우선순위 가이드

```
[높음] ★★★
- Repository 전체
- QuizService, QuizController
- AiRecommendationService
- 초기 데이터 (Flyway)

[중간] ★★
- ProductService, ProductController
- RecommendationService, OutfitController
- DTO 전체

[낮음] ★
- OAuth2 인증 (테스트 시 하드코딩 가능)
- 저장된 코디 기능
- Swagger, 테스트
```
