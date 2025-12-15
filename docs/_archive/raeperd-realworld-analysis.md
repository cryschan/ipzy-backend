# raeperd/realworld-springboot-java 코드 분석

> 분석일: 2025-12-13
> 저장소: https://github.com/raeperd/realworld-springboot-java
> 로컬 경로: `/Users/chan/workspace/raeperd-realworld`

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 기술 스택 | Java 11, Spring Boot 2.5.2, Spring Data JPA, Spring Security |
| 빌드 도구 | Gradle + JaCoCo |
| DB | H2 (인메모리) |
| 테스트 | JUnit 5 + Mockito + BDDMockito |

## 2. 패키지 구조

```
src/main/java/io/github/raeperd/realworld/
├── RealWorldApplication.java
├── application/           # Spring 통합 계층
│   ├── security/         # JWT 인증 필터, Security 설정
│   ├── user/             # UserController, DTO
│   ├── article/          # ArticleController, DTO
│   └── tag/              # TagController
├── domain/               # 비즈니스 로직 (POJO)
│   ├── user/             # User, Email, Password, Profile, UserService
│   ├── article/          # Article, ArticleService
│   └── jwt/              # JWT 인터페이스 (JWTSerializer, JWTDeserializer)
└── infrastructure/       # 기술 구현
    ├── jwt/              # JWT 구현체 (HmacSHA256JWTService)
    └── repository/       # JPA 설정
```

## 3. 핵심 설계 패턴

### 3.1 Value Object로 원시 타입 감싸기

**Email.java** - 이메일을 별도 클래스로 캡슐화
```java
@Embeddable
public class Email {
    @Column(name = "email", nullable = false)
    private String address;

    // equals, hashCode 구현
}
```

**Password.java** - 비밀번호 인코딩 로직 캡슐화
```java
@Embeddable
class Password {  // package-private!
    private String encodedPassword;

    static Password of(String rawPassword, PasswordEncoder encoder) {
        return new Password(encoder.encode(rawPassword));
    }

    boolean matchesPassword(String rawPassword, PasswordEncoder encoder) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
```

**IPZY 적용 포인트:**
- `User.email`을 `Email` Value Object로 분리 고려
- 비밀번호가 있다면 `Password` 클래스로 인코딩 로직 캡슐화

### 3.2 Package-Private 접근 제어

```java
// Controller - package-private
@RestController
class UserRestController { ... }

// Value Object - package-private
@Embeddable
class Password { ... }

// Entity 내부 메서드 - package-private
void changeEmail(Email email) { ... }
void changeName(UserName userName) { ... }
```

**장점:**
- 불필요한 public 노출 방지
- 같은 패키지 내에서만 접근 가능 → 캡슐화 강화
- 테스트는 같은 패키지에 위치하므로 접근 가능

### 3.3 인터페이스 분리 (의존성 역전)

**Domain 계층 - 인터페이스 정의**
```java
// domain/jwt/JWTSerializer.java
public interface JWTSerializer {
    String jwtFromUser(User user);
}

// domain/user/UserFindService.java
public interface UserFindService {
    Optional<User> findById(long id);
    Optional<User> findByUsername(UserName userName);
}
```

**Infrastructure 계층 - 구현 제공**
```java
// infrastructure/jwt/HmacSHA256JWTService.java
class HmacSHA256JWTService implements JWTSerializer, JWTDeserializer {
    // 실제 JWT 생성/검증 로직
}
```

**장점:**
- Domain이 Infrastructure에 의존하지 않음
- JWT 라이브러리 교체 시 Infrastructure만 수정
- 테스트 시 Mock 구현체 주입 용이

### 3.4 Optional을 활용한 Update Request

```java
public class UserUpdateRequest {
    private final Email emailToUpdate;
    private final UserName userNameToUpdate;
    // ...

    Optional<Email> getEmailToUpdate() {
        return ofNullable(emailToUpdate);
    }
}

// Service에서 사용
request.getEmailToUpdate().ifPresent(user::changeEmail);
request.getUserNameToUpdate().ifPresent(user::changeName);
```

**장점:**
- null 체크 없이 깔끔한 업데이트 로직
- 부분 업데이트 자연스럽게 지원

### 3.5 Entity 메서드를 통한 비즈니스 로직

```java
@Entity
public class User {
    // 글 작성 - User가 Article 생성
    public Article writeArticle(ArticleContents contents) {
        return new Article(this, contents);
    }

    // 댓글 작성
    public Comment writeCommentToArticle(Article article, String body) {
        return article.addComment(this, body);
    }

    // 좋아요
    public Article favoriteArticle(Article articleToFavorite) {
        articleFavorited.add(articleToFavorite);
        return articleToFavorite.afterUserFavoritesArticle(this);
    }
}
```

**장점:**
- 도메인 로직이 Entity에 응집
- Service는 조율(orchestration) 역할만 수행

## 4. 테스트 전략

### 4.1 JaCoCo 커버리지 검증 (test.gradle)

```groovy
jacocoTestCoverageVerification {
    violationRules {
        // 모든 클래스의 모든 메서드 테스트 필수
        rule {
            element = "CLASS"
            limit {
                counter = 'METHOD'
                value = 'COVEREDRATIO'
                minimum = 1.00
            }
        }

        // 클래스당 최대 100줄
        rule {
            element = "CLASS"
            limit {
                counter = 'LINE'
                value = 'TOTALCOUNT'
                maximum = 100
            }
        }

        // 메서드당 최대 15줄
        rule {
            element = "METHOD"
            limit {
                counter = 'LINE'
                value = 'TOTALCOUNT'
                maximum = 15
            }
        }
    }
}
```

### 4.2 BDDMockito 스타일 테스트

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Test
    void when_login_expect_user_matches_password(@Mock Email email, @Mock User user) {
        // Given
        given(userRepository.findFirstByEmail(email)).willReturn(of(user));

        // When
        userService.login(email, "raw-password");

        // Then
        then(user).should(times(1)).matchesPassword("raw-password", passwordEncoder);
    }
}
```

## 5. IPZY 적용 가능 포인트

### 5.1 즉시 적용 가능 (Low Effort)

| 패턴 | 현재 IPZY | 적용 방법 |
|------|-----------|----------|
| **Package-private Controller** | public | `public` 제거 |
| **final 필드** | 일부 사용 | Service, Controller 필드에 final 추가 |
| **Optional 업데이트** | 직접 null 체크 | `ifPresent()` 패턴 적용 |

### 5.2 중기 적용 (Medium Effort)

| 패턴 | 설명 | 적용 대상 |
|------|------|----------|
| **Value Object** | 원시 타입 래핑 | Email, ProviderId 등 |
| **JaCoCo 도입** | 테스트 커버리지 강제 | build.gradle |
| **인터페이스 분리** | 조회 전용 인터페이스 | UserFindService 패턴 |

### 5.3 참고만 (IPZY와 맥락 다름)

| 패턴 | 이유 |
|------|------|
| JWT 자체 구현 | IPZY는 Session 기반 OAuth2 사용 |
| Domain Lombok 금지 | 현재 Lombok 활용 중, 전환 비용 높음 |

## 6. 코드 예시 비교

### Controller 비교

**raeperd (Package-private + 생성자 주입)**
```java
@RestController
class UserRestController {
    private final UserService userService;

    UserRestController(UserService userService) {
        this.userService = userService;
    }
}
```

**IPZY 현재**
```java
@RestController
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
}
```

**개선 제안:** `public` 제거 가능 (같은 패키지 내 테스트 접근 가능)

### Service 비교

**raeperd**
```java
@Service
public class UserService implements UserFindService {
    @Transactional
    public User updateUser(long id, UserUpdateRequest request) {
        final var user = userRepository.findById(id).orElseThrow(NoSuchElementException::new);
        request.getEmailToUpdate().ifPresent(user::changeEmail);
        // ...
        return userRepository.save(user);
    }
}
```

**IPZY 현재 (이미 유사)**
```java
@Service
@Transactional(readOnly = true)
public class UserService {
    @Transactional
    public User updateProfile(Long userId, String name, String phone, String profileImageUrl) {
        User user = findActiveUser(userId);
        user.updateProfile(name, phone, profileImageUrl);
        return user;
    }
}
```

## 7. 결론

raeperd 프로젝트에서 IPZY에 **가장 유용한 참고 포인트**:

1. **Package-private 활용** - 불필요한 public 노출 줄이기
2. **Optional 업데이트 패턴** - null 체크 없는 깔끔한 코드
3. **JaCoCo 커버리지 강제** - 테스트 품질 보장
4. **인터페이스 분리** - 조회 전용 서비스 인터페이스
5. **클래스/메서드 라인 제한** - 코드 복잡도 관리

## 8. 참고 파일 위치

| 파일 | 경로 |
|------|------|
| User Entity | `domain/user/User.java` |
| UserService | `domain/user/UserService.java` |
| Value Objects | `domain/user/Email.java`, `Password.java` |
| Controller | `application/user/UserRestController.java` |
| 테스트 | `test/.../domain/user/UserServiceTest.java` |
| JaCoCo 설정 | `test.gradle` |
