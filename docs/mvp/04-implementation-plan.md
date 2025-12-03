# 구현 계획

## 개요

MVP 구현을 3개의 Phase로 나누어 진행합니다.

```
Phase 1: 핵심 플로우 (퀴즈 → 추천)
Phase 2: 인증 + 저장 기능
Phase 3: 마무리 및 테스트
```

---

## Phase 1: 핵심 플로우

### 1.1 Repository 및 Service 기본 틀

**목표**: 데이터 접근 레이어 구축

#### Quiz 도메인

```java
// QuizRepository.java
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    Optional<Quiz> findByIdAndIsActiveTrue(Long id);
    List<Quiz> findAllByIsActiveTrueOrderByDisplayOrderAsc();
}

// QuizQuestionRepository.java
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
}

// QuizSessionRepository.java
public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {
    Optional<QuizSession> findByIdAndUserId(Long id, Long userId);
    List<QuizSession> findByUserId(Long userId);
}

// QuizAnswerRepository.java
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
}
```

#### Product 도메인

```java
// ProductRepository.java
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByIsActiveTrue();
    List<Product> findByCategoryAndIsActiveTrue(ClothingCategory category);
}

// BrandRepository.java
public interface BrandRepository extends JpaRepository<Brand, Long> {
}
```

#### Recommendation 도메인

```java
// RecommendationRepository.java
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findBySessionIdOrderByDisplayOrderAsc(Long sessionId);
    List<Recommendation> findByUserId(Long userId);
}

// RecommendationItemRepository.java
public interface RecommendationItemRepository extends JpaRepository<RecommendationItem, Long> {
}
```

---

### 1.2 퀴즈 API 구현

**목표**: 퀴즈 조회 및 답변 제출

#### QuizController.java

```java
@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping("/{quizId}")
    public ApiResponse<QuizDetailResponse> getQuiz(@PathVariable Long quizId) {
        return ApiResponse.success(quizService.getQuizDetail(quizId));
    }

    @PostMapping("/{quizId}/sessions")
    public ApiResponse<QuizSessionResponse> startSession(
            @PathVariable Long quizId,
            @AuthenticationPrincipal User user) {
        return ApiResponse.success(quizService.startSession(quizId, user));
    }

    @PostMapping("/sessions/{sessionId}/submit")
    public ApiResponse<QuizSubmitResponse> submitAnswers(
            @PathVariable Long sessionId,
            @RequestBody @Valid QuizSubmitRequest request,
            @AuthenticationPrincipal User user) {
        return ApiResponse.success(quizService.submitAnswers(sessionId, request, user));
    }

    @PostMapping("/sessions/{sessionId}/recommend")
    public ApiResponse<List<RecommendationSummaryResponse>> generateRecommendations(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User user) {
        return ApiResponse.success(quizService.generateRecommendations(sessionId, user));
    }
}
```

#### DTO 설계

```java
// QuizDetailResponse.java
public record QuizDetailResponse(
    Long id,
    String title,
    String description,
    List<QuestionResponse> questions
) {}

// QuestionResponse.java
public record QuestionResponse(
    Long id,
    String text,
    QuizType type,
    Boolean required,
    Integer displayOrder,
    List<OptionResponse> options
) {}

// QuizSubmitRequest.java
public record QuizSubmitRequest(
    @NotEmpty List<AnswerRequest> answers
) {}

// AnswerRequest.java
public record AnswerRequest(
    @NotNull Long questionId,
    @NotEmpty List<String> selectedOptions
) {}
```

---

### 1.3 AI 추천 연동

**목표**: Spring AI를 통한 OpenAI 연동

#### 의존성 추가

```groovy
// build.gradle
implementation 'org.springframework.ai:spring-ai-openai-spring-boot-starter:0.8.1'
```

#### application.yml

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        model: gpt-4o-mini
        temperature: 0.7
```

#### AiRecommendationService.java

```java
@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final ChatClient chatClient;
    private final ProductRepository productRepository;

    public List<RecommendationResult> generateRecommendations(
            List<QuizAnswer> answers,
            List<Product> availableProducts) {

        String prompt = buildPrompt(answers, availableProducts);

        String response = chatClient.prompt()
            .user(prompt)
            .call()
            .content();

        return parseResponse(response);
    }

    private String buildPrompt(List<QuizAnswer> answers, List<Product> products) {
        // 프롬프트 구성
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 패션 코디네이터입니다.\n\n");
        sb.append("사용자 스타일 분석 결과:\n");

        for (QuizAnswer answer : answers) {
            sb.append("- ").append(answer.getQuestion().getText())
              .append(": ").append(answer.getSelectedOptions())
              .append("\n");
        }

        sb.append("\n사용 가능한 상품 목록:\n");
        sb.append(formatProducts(products));

        sb.append("\n위 정보를 바탕으로 3개의 코디를 추천해주세요.");
        sb.append("각 코디는 TOP, BOTTOM, SHOES를 포함해야 합니다.");
        sb.append("JSON 형식으로 응답해주세요.");

        return sb.toString();
    }
}
```

---

### 1.4 추천 결과 API

**목표**: 추천 코디 및 상품 조회

#### OutfitController.java

```java
@RestController
@RequestMapping("/api/outfits")
@RequiredArgsConstructor
public class OutfitController {

    private final OutfitService outfitService;

    @GetMapping("/{outfitId}")
    public ApiResponse<OutfitDetailResponse> getOutfit(
            @PathVariable Long outfitId) {
        return ApiResponse.success(outfitService.getDetail(outfitId));
    }

    @PostMapping("/{outfitId}/save")
    public ApiResponse<SavedOutfitResponse> saveOutfit(
            @PathVariable Long outfitId,
            @RequestBody SaveOutfitRequest request,
            @AuthenticationPrincipal User user) {
        return ApiResponse.success(outfitService.save(outfitId, request, user));
    }

    @GetMapping("/saved")
    public ApiResponse<Page<SavedOutfitResponse>> getSavedOutfits(
            @AuthenticationPrincipal User user,
            Pageable pageable) {
        return ApiResponse.success(outfitService.getSavedByUser(user, pageable));
    }

    @DeleteMapping("/saved/{savedOutfitId}")
    public ApiResponse<Void> deleteSavedOutfit(
            @PathVariable Long savedOutfitId,
            @AuthenticationPrincipal User user) {
        outfitService.deleteSaved(savedOutfitId, user);
        return ApiResponse.success(null, "삭제되었습니다");
    }
}
```

#### ProductController.java

```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    public ApiResponse<ProductDetailResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.success(productService.getDetail(productId));
    }
}

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public ApiResponse<List<BrandResponse>> getBrands() {
        return ApiResponse.success(brandService.getAll());
    }
}
```

---

## Phase 2: 인증 + 저장 기능

### 2.1 OAuth2 인증 구현

#### 의존성 추가

```groovy
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
```

#### application.yml

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            redirect-uri: "{baseUrl}/api/auth/kakao/callback"
            authorization-grant-type: authorization_code
            client-authentication-method: client_secret_post
            scope:
              - profile_nickname
              - profile_image
              - account_email
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id

app:
  oauth2:
    success-redirect-uri: ${FRONTEND_URL}/auth/callback
    failure-redirect-uri: ${FRONTEND_URL}/auth/callback?error=true
```

#### CustomOAuth2UserService.java

```java
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(provider, oauth2User.getAttributes());

        User user = userRepository.findByProviderAndProviderId(provider, userInfo.getId())
            .orElseGet(() -> createUser(userInfo, provider));

        return new CustomUserPrincipal(user, oauth2User.getAttributes());
    }

    private User createUser(OAuth2UserInfo userInfo, AuthProvider provider) {
        User user = User.builder()
            .email(userInfo.getEmail())
            .name(userInfo.getName())
            .profileImageUrl(userInfo.getImageUrl())
            .provider(provider)
            .providerId(userInfo.getId())
            .role(Role.USER)
            .build();

        return userRepository.save(user);
    }
}
```

#### OAuth2SuccessHandler.java

```java
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${app.oauth2.success-redirect-uri}")
    private String successRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        getRedirectStrategy().sendRedirect(request, response, successRedirectUri);
    }
}
```

#### AuthController.java

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.success(UserResponse.from(principal.getUser()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.success(null, "로그아웃되었습니다");
    }
}
```

---

### 2.3 저장된 코디 관리

OutfitController에 이미 통합됨 (1.4 참조):
- `GET /api/outfits/saved` - 저장된 코디 목록
- `DELETE /api/outfits/saved/{id}` - 저장된 코디 삭제

---

## Phase 3: 마무리

### 3.1 Swagger 설정

```java
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("IPZY API")
                .description("AI 패션 코디 추천 서비스 API")
                .version("v1.0.0"));
    }
}
```

### 3.2 테스트

#### 통합 테스트 시나리오

```java
@SpringBootTest
@AutoConfigureMockMvc
class QuizFlowIntegrationTest {

    @Test
    void 회원_퀴즈_완료_후_추천_받기() {
        // 1. 회원가입/로그인
        // 2. 퀴즈 조회
        // 3. 세션 시작
        // 4. 답변 제출
        // 5. 추천 생성
        // 6. 추천 조회
    }

    @Test
    void 추천_코디_저장_및_조회() {
        // 1. 로그인
        // 2. 퀴즈 완료 → 추천 받기
        // 3. 코디 저장
        // 4. 저장된 코디 목록 조회
    }
}
```

### 3.3 초기 데이터

```sql
-- V2__insert_initial_quiz.sql
INSERT INTO quizzes (title, description, is_active, display_order) VALUES
('나의 스타일 찾기', '10가지 질문으로 당신의 스타일을 분석합니다', true, 1);

INSERT INTO quiz_questions (quiz_id, text, type, display_order, required) VALUES
(1, '선호하는 스타일은?', 'SINGLE', 1, true),
(1, '주로 입는 상황은?', 'MULTIPLE', 2, true);
-- ...
```

---

## 체크리스트

### Phase 1
- [ ] QuizRepository, QuizService
- [ ] ProductRepository, ProductService
- [ ] RecommendationRepository, RecommendationService
- [ ] QuizController (GET, POST sessions, POST submit)
- [ ] Spring AI 설정
- [ ] AiRecommendationService
- [ ] RecommendationController (GET)
- [ ] ProductController (GET)

### Phase 2
- [ ] SecurityConfig (OAuth2 설정)
- [ ] CustomOAuth2UserService
- [ ] OAuth2SuccessHandler / FailureHandler
- [ ] AuthController (GET /me, POST /logout)
- [ ] OutfitController, SavedOutfitService
- [ ] User 엔티티 (provider, providerId 필드)

### Phase 3
- [ ] Swagger 설정
- [ ] 통합 테스트
- [ ] 초기 데이터 (Flyway)
- [ ] 에러 케이스 처리
- [ ] API 문서 검토
