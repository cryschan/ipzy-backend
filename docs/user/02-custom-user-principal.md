# CustomUserPrincipal 구현

## 개요

인증된 사용자 정보에 쉽게 접근하기 위한 `CustomUserPrincipal` 클래스 구현.

**Before:**
```java
Long userId = ((Number) principal.getAttributes().get("userId")).longValue();
```

**After:**
```java
Long userId = principal.getUserId();
```

---

## 문제 / 대안 / 선택

### 문제

인증된 사용자 정보 접근 시 **Map 캐스팅 필요**

```java
// 타입 안전성 ❌, 런타임 오류 위험
Long userId = ((Number) principal.getAttributes().get("userId")).longValue();
String email = (String) principal.getAttributes().get("email");
```

- 문자열 키 오타 시 런타임 오류
- 캐스팅 실수 가능
- IDE 자동완성 미지원

### 대안 비교

| 방식 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **DefaultOAuth2User** | Spring 기본 제공, Map 기반 | 추가 코드 없음 | 타입 안전성 ❌ |
| **CustomUserPrincipal** | 강타입 객체, getter 제공 | 타입 안전, IDE 지원 | 클래스 추가 필요 |

### 선택: CustomUserPrincipal

- 타입 안전한 getter로 컴파일 타임 검증
- IDE 자동완성 지원
- 책임 분리 (서비스 → DTO로 생성 로직 이동)

---

## 클래스 구조

### CustomUserPrincipal

```java
@Getter
public class CustomUserPrincipal implements OAuth2User, Serializable {

    private final Long userId;
    private final String email;
    private final String userName;  // OAuth2User.getName()과 충돌 방지
    private final String profileImageUrl;
    private final UserRole role;
    private final Map<String, Object> attributes;

    // OAuth2User 구현
    @Override
    public String getName() { return String.valueOf(userId); }  // Spring Security 식별자
}
```

**파일 위치:** `domain/auth/dto/CustomUserPrincipal.java`

---

## 사용 방법

### Controller에서 사용

```java
@RestController
@RequestMapping("/api/quiz-sessions")
@RequiredArgsConstructor
public class QuizSessionController {

    private final QuizSessionService quizSessionService;

    @PostMapping
    public ApiResponse<QuizSessionResponse> createSession(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        Long userId = principal.getUserId();

        return ApiResponse.success(quizSessionService.create(userId));
    }
}
```

### 사용 가능한 메서드

| 메서드 | 반환 타입 | 설명 |
|--------|----------|------|
| `getUserId()` | Long | 사용자 ID |
| `getEmail()` | String | 이메일 |
| `getUserName()` | String | 사용자 이름 |
| `getProfileImageUrl()` | String | 프로필 이미지 URL |
| `getRole()` | UserRole | 권한 (USER, ADMIN) |

---

## 주의사항

### getName() vs getUserName()

| 메서드 | 용도 | 반환값 예시 |
|--------|------|------------|
| `getName()` | Spring Security 식별자 (OAuth2User 인터페이스) | `"1"` |
| `getUserName()` | 실제 사용자 이름 | `"홍길동"` |

**사용자 이름이 필요하면 반드시 `getUserName()`을 사용하세요.**

---

## 사용 예시

### 1. 리소스 생성 시 소유자 지정

```java
@PostMapping("/saved-outfits")
public ApiResponse<SavedOutfitResponse> saveOutfit(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        @RequestBody SaveOutfitRequest request) {

    return ApiResponse.success(
        outfitService.save(principal.getUserId(), request)
    );
}
```

### 2. 내 리소스 조회

```java
@GetMapping("/my-recommendations")
public ApiResponse<List<RecommendationResponse>> getMyRecommendations(
        @AuthenticationPrincipal CustomUserPrincipal principal) {

    return ApiResponse.success(
        recommendationService.findByUserId(principal.getUserId())
    );
}
```

### 3. 소유권 검증

```java
@GetMapping("/quiz-sessions/{sessionId}")
public ApiResponse<QuizSessionResponse> getSession(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        @PathVariable Long sessionId) {

    return ApiResponse.success(
        quizSessionService.findByIdAndUserId(sessionId, principal.getUserId())
    );
}
```

### 4. 권한 체크

```java
@DeleteMapping("/admin/users/{userId}")
public ApiResponse<Void> deleteUser(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        @PathVariable Long userId) {

    if (principal.getRole() != UserRole.ADMIN) {
        throw AuthException.forbidden();
    }

    userService.delete(userId);
    return ApiResponse.success(null);
}
```

---

## Service 레이어 패턴

### 소유권 검증 포함 조회

```java
@Service
@RequiredArgsConstructor
public class QuizSessionService {

    private final QuizSessionRepository repository;

    public QuizSession findByIdAndUserId(Long sessionId, Long userId) {
        QuizSession session = repository.findById(sessionId)
            .orElseThrow(() -> new QuizException(QUIZ_SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) {
            throw AuthException.forbidden();
        }

        return session;
    }
}
```

---

## 변경된 파일 목록

| 파일 | 작업 |
|------|------|
| `domain/auth/dto/CustomUserPrincipal.java` | 신규 생성 |
| `domain/auth/dto/AuthMeResponse.java` | CustomUserPrincipal 사용 |
| `domain/auth/service/CustomOAuth2UserService.java` | CustomUserPrincipal 반환 |
| `domain/auth/controller/AuthController.java` | CustomUserPrincipal 주입 |

---

## 설계 결정

1. **User 엔티티 직접 참조 안함**
   - 세션 직렬화 문제 방지
   - 지연 로딩 이슈 방지

2. **필요한 필드만 복사**
   - userId, email, userName, profileImageUrl, role

3. **Serializable 구현**
   - 세션 저장 호환성 확보
   - Redis 세션, 클러스터링 환경 대비
   - 객체를 바이트로 변환하여 다른 서버/저장소로 전송 가능
   - 미구현 시 `NotSerializableException` 발생

4. **userName 필드명 사용**
   - OAuth2User.getName()과의 충돌 방지
