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
| GET | `/api/auth/kakao` | 카카오 로그인 시작 | X |
| GET | `/api/auth/kakao/callback` | 카카오 콜백 | X |
| GET | `/api/auth/me` | 현재 로그인 사용자 | O |
| POST | `/api/auth/logout` | 로그아웃 | O |

### 사용자 API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|:----:|
| GET | `/api/users/me` | 내 정보 조회 | O |

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

### Step 3: Auth DTO 생성

| 파일 | 용도 |
|------|------|
| `OAuth2UserInfo.java` | OAuth 사용자 정보 인터페이스 |
| `KakaoOAuth2UserInfo.java` | 카카오 응답 파싱 |
| `OAuth2UserInfoFactory.java` | Provider별 UserInfo 생성 |
| `CustomUserPrincipal.java` | Spring Security Principal |

#### OAuth2UserInfo.java

```java
public interface OAuth2UserInfo {
    String getId();
    String getEmail();
    String getName();
    String getImageUrl();
}
```

#### KakaoOAuth2UserInfo.java

```java
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;

    @Override
    public String getId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        return (String) kakaoAccount.get("email");
    }

    @Override
    public String getName() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        return (String) properties.get("nickname");
    }

    @Override
    public String getImageUrl() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        return (String) properties.get("profile_image");
    }
}
```

#### CustomUserPrincipal.java

```java
public class CustomUserPrincipal implements OAuth2User, UserDetails {
    private final User user;
    private final Map<String, Object> attributes;

    // OAuth2User 구현
    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getName() { return String.valueOf(user.getId()); }

    // UserDetails 구현
    @Override
    public String getUsername() { return user.getEmail(); }

    @Override
    public String getPassword() { return null; }

    // Getter
    public User getUser() { return user; }
    public Long getUserId() { return user.getId(); }
}
```

---

### Step 4: OAuth2UserService 구현

**파일:** `domain/auth/service/CustomOAuth2UserService.java`

```java
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String provider = registrationId.toUpperCase();

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.create(provider, oauth2User.getAttributes());

        User user = userRepository.findByProviderAndProviderId(provider, userInfo.getId())
            .map(existingUser -> updateExistingUser(existingUser, userInfo))
            .orElseGet(() -> createUser(provider, userInfo));

        return new CustomUserPrincipal(user, oauth2User.getAttributes());
    }

    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {
        user.updateLastLoginAt();
        // 필요시 프로필 업데이트
        return user;
    }

    private User createUser(String provider, OAuth2UserInfo userInfo) {
        User user = User.builder()
            .email(userInfo.getEmail())
            .name(userInfo.getName())
            .profileImageUrl(userInfo.getImageUrl())
            .provider(provider)
            .providerId(userInfo.getId())
            .role(UserRole.USER)
            .status(UserStatus.ACTIVE)
            .build();

        return userRepository.save(user);
    }
}
```

---

### Step 5: Handler 구현

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

### Step 6: SecurityConfig 수정

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
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(5)
                .maxSessionsPreventsLogin(false)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/quizzes/**").permitAll()
                .requestMatchers("/api/products/**").permitAll()
                .requestMatchers("/api/brands/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
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
                .failureHandler(oAuth2FailureHandler)
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }
}
```

---

### Step 7: Controller 구현

#### AuthController.java

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        if (principal == null) {
            throw new AuthException(AuthErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.success(UserResponse.from(principal.getUser()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.success(null, "로그아웃되었습니다");
    }
}
```

#### UserController.java

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.success(UserResponse.from(principal.getUser()));
    }
}
```

---

### Step 8: Response DTO

#### UserResponse.java

```java
public record UserResponse(
    Long id,
    String email,
    String name,
    String profileImageUrl,
    String provider,
    String role,
    LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getProfileImageUrl(),
            user.getProvider(),
            user.getRole().name(),
            user.getCreatedAt()
        );
    }
}
```

---

### Step 9: application.yml 설정

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
| 1 | 수정 | `domain/user/entity/User.java` | [x] |
| 2 | 수정 | `domain/user/repository/UserRepository.java` | [x] |
| 3 | 생성 | `domain/auth/service/CustomOAuth2UserService.java` | [x] |
| 4 | 수정 | `global/config/SecurityConfig.java` | [x] |
| 5 | 생성 | `domain/auth/controller/AuthController.java` | [x] |
| 6 | 생성 | `domain/auth/dto/AuthMeResponse.java` | [x] |
| 7 | 수정 | `resources/application.yml` | [x] |

**참고:** OAuth2UserInfo 인터페이스 패턴 대신 CustomOAuth2UserService에서 카카오 응답을 직접 파싱하는 단순화된 구조로 구현됨. 향후 다른 Provider 추가 시 인터페이스 분리 검토 필요.

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
| Config | 1 | 1 | 100% |
| Controller | 1 | 1 | 100% |
| **합계** | **7** | **7** | **100%** |
