# CustomUserPrincipal 도입

## 개요

Spring Security OAuth2 인증 후 사용자 정보에 타입 안전하게 접근하기 위한 커스텀 Principal 클래스

---

## 문제

인증된 사용자 정보 접근 시 **Map 캐스팅 필요**

```java
// Before: 타입 안전성 ❌, 런타임 오류 위험
Long userId = ((Number) principal.getAttributes().get("userId")).longValue();
String email = (String) principal.getAttributes().get("email");
```

- 문자열 키 오타 시 런타임 오류
- 캐스팅 실수 가능
- IDE 자동완성 미지원

---

## 대안 비교

| 방식 | 설명 |
|------|------|
| **DefaultOAuth2User** | Spring 기본 제공, Map 기반 attributes |
| **CustomUserPrincipal** | 강타입 객체, getter 메서드 제공 |

---

## 선택: CustomUserPrincipal

### 클래스 구조

```java
@Getter
public class CustomUserPrincipal implements OAuth2User, Serializable {

    private final Long userId;
    private final String email;
    private final String userName;       // OAuth2User.getName()과 충돌 방지
    private final String profileImageUrl;
    private final UserRole role;
    private final Map<String, Object> attributes;

    // OAuth2User 구현
    @Override
    public String getName() {
        return String.valueOf(userId);  // Spring Security 식별자
    }
}
```

### 구현 인터페이스

| 인터페이스 | 목적 |
|-----------|------|
| `OAuth2User` | Spring Security OAuth2 인증 시스템 통합 |
| `Serializable` | 세션 직렬화 (Redis, 클러스터링 지원) |

---

## 변경 내용

### 1. CustomOAuth2UserService

```java
// Before: 서비스에서 Map 조립
Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
attributes.put("userId", user.getId());
attributes.put("email", email);
return new DefaultOAuth2User(authorities, attributes, "id");

// After: 생성 책임을 Principal로 위임
return CustomUserPrincipal.from(user, oauth2User.getAttributes());
```

### 2. AuthController

```java
// Before
public ApiResponse<AuthMeResponse> me(@AuthenticationPrincipal OAuth2User principal)

// After
public ApiResponse<AuthMeResponse> me(@AuthenticationPrincipal CustomUserPrincipal principal)
```

### 3. AuthMeResponse

```java
// Before: Map에서 캐스팅하며 추출
Long userId = ((Number) attributes.get("userId")).longValue();

// After: getter로 직접 접근
principal.getUserId();
```

---

## 성과

### Before vs After

```java
// Before
Long userId = ((Number) principal.getAttributes().get("userId")).longValue();

// After
Long userId = principal.getUserId();
```

### 개선점

| 항목 | Before | After |
|------|--------|-------|
| 타입 안전성 | 런타임 캐스팅 | 컴파일 타임 검증 |
| IDE 지원 | 자동완성 불가 | 자동완성 지원 |
| 코드 가독성 | Map 키 문자열 | 명확한 메서드명 |
| 책임 분리 | 서비스에서 DTO 조립 | 각자 역할 분리 |

---

## 설계 결정

| 결정 | 이유 |
|------|------|
| User 엔티티 미참조 | 세션 직렬화 시 LazyInitializationException 방지 |
| `userName` 필드명 | `getName()`은 OAuth2User 인터페이스가 사용 |
| `final` 필드 | 불변 객체로 스레드 안전성 확보 |
| `Serializable` 구현 | Redis 세션, 클러스터링 대비 |

---

## 사용 예시

### Controller에서 사용

```java
@GetMapping("/me")
public ApiResponse<AuthMeResponse> me(
        @AuthenticationPrincipal CustomUserPrincipal principal) {

    Long userId = principal.getUserId();
    String name = principal.getUserName();
    UserRole role = principal.getRole();

    return ApiResponse.success(AuthMeResponse.from(principal));
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
| `getName()` | Spring Security 식별자 | `"1"` |
| `getUserName()` | 실제 사용자 이름 | `"홍길동"` |

**사용자 이름이 필요하면 반드시 `getUserName()`을 사용**
