# User 도메인 구현 계획

## 개요

User 도메인은 핵심 플로우(로그인 → 퀴즈 → AI 추천)에서 **사용자 식별과 권한 검증**을 담당합니다.

```
로그인 → 퀴즈 시작 → 퀴즈 답변 제출 → AI 추천 생성 → 결과 조회
   ↑         ↑              ↑              ↑
   └─────────┴──────────────┴──────────────┘
              User 도메인 (인증/인가)
```

---

## 필수 기능

| 우선순위 | 기능 | 설명 |
|:--------:|------|------|
| P0 | OAuth 로그인 | 카카오 로그인으로 사용자 생성/조회 |
| P0 | 현재 사용자 조회 | 세션에서 로그인 사용자 정보 반환 |
| P0 | 로그아웃 | 세션 무효화 |
| P0 | 소유권 검증 | 퀴즈 세션/저장 코디 접근 권한 확인 |

---

## ERD (users 테이블)

```sql
CREATE TABLE users (
    id                BIGSERIAL PRIMARY KEY,
    email             VARCHAR(255) NOT NULL UNIQUE,
    name              VARCHAR(100) NOT NULL,
    profile_image_url VARCHAR(500),
    provider          VARCHAR(20) NOT NULL,  -- KAKAO, NAVER, GOOGLE
    provider_id       VARCHAR(100) NOT NULL,
    role              VARCHAR(20) DEFAULT 'USER',
    status            VARCHAR(20) DEFAULT 'ACTIVE',
    last_login_at     TIMESTAMP,
    deleted_at        TIMESTAMP,
    created_at        TIMESTAMP DEFAULT NOW(),
    modified_at       TIMESTAMP DEFAULT NOW(),

    UNIQUE(provider, provider_id)
);
```

**설계 결정:**
- `password` 필드 제거 (OAuth 전용)
- `provider` + `provider_id` 복합 유니크 제약

---

## API 명세

### 인증 API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|:----:|
| GET | `/api/auth/login/kakao` | 카카오 로그인 시작 (→ `/oauth2/authorization/kakao` 리다이렉트) | X |
| GET | `/api/auth/kakao/callback` | 카카오 콜백 (Spring Security 자동 처리) | X |
| GET | `/api/auth/me` | 현재 로그인 사용자 | O |
| POST | `/api/auth/logout` | 로그아웃 | O |

---

## 구현 순서

### Step 1: Entity 수정 ✅ 완료

**파일:** `domain/user/entity/User.java`

**변경 내용:**
```java
// 제거
- private String password;
- public void updatePassword(String encodedPassword);

// 추가
+ @Column(nullable = false, length = 20)
+ private String provider;

+ @Column(name = "provider_id", nullable = false, length = 100)
+ private String providerId;

// Table 유니크 제약 추가
+ @Table(name = "users", uniqueConstraints = {
+     @UniqueConstraint(columnNames = {"provider", "provider_id"})
+ })
```

**결과물:**
- [x] provider 필드 추가
- [x] providerId 필드 추가
- [x] password 필드 제거
- [x] updatePassword 메서드 제거
- [x] 복합 유니크 제약 추가

---

### Step 2: Repository 확장

**파일:** `domain/user/repository/UserRepository.java`

**추가 메서드:**
```java
Optional<User> findByProviderAndProviderId(String provider, String providerId);
```

---

### Step 3: Auth DTO 생성 ✅ 완료

**설계 결정:** `CustomUserPrincipal` 대신 `DefaultOAuth2User`를 사용하여 단순화.
- userId를 attributes에 포함시켜 세션에서 조회 가능
- 향후 다른 Provider 추가 시 인터페이스 분리 검토

| 파일 | 용도 |
|------|------|
| `AuthMeResponse.java` | 현재 로그인 사용자 응답 DTO |

#### AuthMeResponse.java

```java
public record AuthMeResponse(
    Long id,
    String email,
    String name,
    String profileImageUrl
) {
    public static AuthMeResponse from(OAuth2User principal) {
        Map<String, Object> attributes = principal.getAttributes();
        return new AuthMeResponse(
            ((Number) attributes.get("userId")).longValue(),
            (String) attributes.get("email"),
            (String) attributes.get("name"),
            (String) attributes.get("profileImageUrl")
        );
    }
}
```

---

### Step 4: OAuth2UserService 구현 ✅ 완료

**파일:** `domain/auth/service/CustomOAuth2UserService.java`

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(request);

        String provider = request.getClientRegistration().getRegistrationId().toUpperCase();
        String providerId = String.valueOf(oauth2User.getAttributes().get("id"));

        // 카카오 응답에서 사용자 정보 추출
        Map<String, Object> kakaoAccount = (Map<String, Object>) oauth2User.getAttributes().get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String email = (String) kakaoAccount.get("email");
        String name = (String) profile.get("nickname");
        String profileImage = (String) profile.get("profile_image_url");

        // 사용자 조회 또는 생성
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .map(existingUser -> {
                    existingUser.updateOAuthInfo(name, profileImage);
                    return existingUser;
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .name(name)
                        .profileImageUrl(profileImage)
                        .provider(provider)
                        .providerId(providerId)
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
                        .build()));

        user.updateLastLoginAt();

        // 세션에 저장될 attributes에 userId 추가
        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
        attributes.put("userId", user.getId());
        attributes.put("email", email);
        attributes.put("name", name);

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                attributes,
                "id"
        );
    }
}
```

---

### Step 5: Handler 구현 ✅ 완료

#### OAuth2SuccessHandler.java

```java
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${app.oauth2.success-redirect-uri}")
    private String successRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        getRedirectStrategy().sendRedirect(request, response, successRedirectUri);
    }
}
```

#### OAuth2FailureHandler.java

```java
@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.failure-redirect-uri}")
    private String failureRedirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        getRedirectStrategy().sendRedirect(request, response, failureRedirectUri);
    }
}
```

---

### Step 6: SecurityConfig 수정 ✅ 완료

**파일:** `global/config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(2)
                .maxSessionsPreventsLogin(false)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/me").authenticated()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/quizzes/**").permitAll()
                .requestMatchers("/api/products/**").permitAll()
                .requestMatchers("/api/brands/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(endpoint -> endpoint
                    .baseUri("/oauth2/authorization")  // Spring 기본값 사용
                )
                .redirectionEndpoint(endpoint -> endpoint
                    .baseUri("/api/auth/*/callback")
                )
                .userInfoEndpoint(endpoint -> endpoint
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    // 401 JSON 응답 (세션 만료 / 비로그인 구분)
                })
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> {
                    // 200 JSON 응답
                })
            );

        return http.build();
    }
}
```

**주요 변경 사항:**
- `authorizationEndpoint`: `/api/auth` → `/oauth2/authorization` (Spring 기본값)
- `/api/auth/me`는 authenticated 먼저 체크
- 401 응답을 JSON으로 반환하는 `authenticationEntryPoint` 추가
- 로그아웃 성공 시 JSON 응답 반환

---

### Step 7: Controller 구현 ✅ 완료

#### AuthController.java

```java
@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Operation(summary = "카카오 로그인")
    @GetMapping("/login/kakao")
    public void kakaoLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/kakao");
    }

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ApiResponse<AuthMeResponse> me(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            throw AuthException.unauthorized();
        }
        return ApiResponse.success(AuthMeResponse.from(principal));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public void logout() {
        // Spring Security가 처리 (Swagger 문서화용)
    }
}
```

**변경 사항:**
- `CustomUserPrincipal` → `OAuth2User` 사용
- `UserResponse` → `AuthMeResponse` 사용
- `/api/auth/login/kakao` 엔드포인트 추가
- UserController 제거 (AuthController의 `/me`와 중복)

---

### Step 8: Response DTO ✅ 완료

#### AuthMeResponse.java

```java
public record AuthMeResponse(
    Long id,
    String email,
    String name,
    String profileImageUrl
) {
    public static AuthMeResponse from(OAuth2User principal) {
        Map<String, Object> attributes = principal.getAttributes();
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return new AuthMeResponse(
            ((Number) attributes.get("userId")).longValue(),
            (String) attributes.get("email"),
            (String) attributes.get("name"),
            (String) profile.get("profile_image_url")
        );
    }
}
```

**변경 사항:**
- `UserResponse` → `AuthMeResponse` 이름 변경
- `OAuth2User`의 attributes에서 직접 추출
- provider, role, createdAt 필드 제거 (MVP에 불필요)

---

### Step 9: application.yml 설정 ✅ 완료

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
    success-redirect-uri: ${FRONTEND_URL:http://localhost:3000}/auth/callback
    failure-redirect-uri: ${FRONTEND_URL:http://localhost:3000}/auth/callback?error=true
```

---

## 파일 체크리스트

| 순서 | 작업 | 파일 경로 | 상태 |
|:----:|------|----------|:----:|
| 1 | 수정 | `domain/user/entity/User.java` | ✅ |
| 2 | 수정 | `domain/user/repository/UserRepository.java` | ✅ |
| 3 | 생성 | `domain/auth/service/CustomOAuth2UserService.java` | ✅ |
| 4 | 수정 | `global/config/SecurityConfig.java` | ✅ |
| 5 | 생성 | `domain/auth/controller/AuthController.java` | ✅ |
| 6 | 생성 | `domain/auth/dto/AuthMeResponse.java` | ✅ |
| 7 | 생성 | `domain/auth/handler/OAuth2SuccessHandler.java` | ✅ |
| 8 | 생성 | `domain/auth/handler/OAuth2FailureHandler.java` | ✅ |
| 9 | 생성 | `domain/auth/exception/AuthErrorCode.java` | ✅ |
| 10 | 생성 | `domain/auth/exception/AuthException.java` | ✅ |
| 11 | 수정 | `resources/application.yml` | ✅ |

**설계 결정:**
- `CustomUserPrincipal` 대신 `DefaultOAuth2User` 사용 (단순화)
- `OAuth2UserInfo` 인터페이스 미사용 (카카오 전용, 향후 확장 시 분리)
- `UserController` 미생성 (`AuthController`의 `/me`로 통합)

---

## 의존성 추가 (build.gradle)

```groovy
// OAuth2 Client
implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
```

---

## 테스트 시나리오

### 1. 카카오 로그인 플로우

```
1. GET /api/auth/kakao → 카카오 로그인 페이지 리다이렉트
2. 카카오 로그인 완료 → /api/auth/kakao/callback 호출
3. CustomOAuth2UserService.loadUser() 실행
   - 신규 사용자: User 생성
   - 기존 사용자: lastLoginAt 갱신
4. OAuth2SuccessHandler → 프론트엔드 리다이렉트
5. 세션 쿠키 (JSESSIONID) 설정됨
```

### 2. 인증 확인

```
GET /api/auth/me
- 로그인 상태: 200 + UserResponse
- 비로그인 상태: 401 Unauthorized
```

### 3. 로그아웃

```
POST /api/auth/logout
- 세션 무효화
- JSESSIONID 쿠키 삭제
```

---

## 진행 현황

| 항목 | 전체 | 완료 | 진행률 |
|------|:----:|:----:|:------:|
| Entity/Repository | 2 | 2 | 100% |
| Auth DTO | 1 | 1 | 100% |
| Service | 1 | 1 | 100% |
| Handler | 2 | 2 | 100% |
| Exception | 2 | 2 | 100% |
| Config | 1 | 1 | 100% |
| Controller | 1 | 1 | 100% |
| **합계** | **11** | **11** | **100%** |
