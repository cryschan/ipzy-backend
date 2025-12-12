# IPZY 코드 패턴 가이드

> 작성일: 2025-12-13
> 이 문서는 IPZY 백엔드 프로젝트의 코드 패턴을 정리합니다.

## 1. 전체 아키텍처 흐름

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Controller    │────▶│     Service     │────▶│     Entity      │────▶│   Repository    │
│   (domain)      │     │    (domain)     │     │    (domain)     │     │    (domain)     │
└─────────────────┘     └─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
        ▼                       ▼                       ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Request DTO    │     │  Command DTO    │     │  Value Object   │
│  Response DTO   │     │  (입력 전용)     │     │  (vo 패키지)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### 패키지 구조

```
com.ipzy
├── domain/{도메인}/
│   ├── controller/     # REST API 엔드포인트
│   ├── service/        # 비즈니스 로직
│   ├── repository/     # 데이터 접근
│   ├── entity/         # JPA 엔티티
│   ├── dto/            # Request/Response/Command DTO
│   ├── vo/             # Value Object
│   └── exception/      # 도메인 예외 (ErrorCode + Exception)
└── _global/
    ├── common/         # ApiResponse, BaseEntity, enums
    ├── config/         # SecurityConfig, SwaggerConfig 등
    └── exception/      # BusinessException, GlobalExceptionHandler
```

---

## 2. Controller 패턴

### 2.1 기본 구조

```java
@Tag(name = "User", description = "사용자 API")
@RequiredArgsConstructor
@RequestMapping("/api/users")
@RestController
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회", description = "로그인한 사용자의 프로필을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않음 (AUTH_001)")
    })
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        validatePrincipal(principal);
        return ApiResponse.success(userService.getMyProfile(principal.getUserId()));
    }
}
```

### 2.2 핵심 규칙

| 항목 | 규칙 |
|------|------|
| 클래스 어노테이션 | `@RestController` + `@RequestMapping` + `@RequiredArgsConstructor` |
| Swagger | `@Tag` (클래스), `@Operation` + `@ApiResponses` (메서드) |
| 응답 래퍼 | 항상 `ApiResponse<T>` 사용 |
| 인증 | `@AuthenticationPrincipal CustomUserPrincipal` |
| DELETE 응답 | `ResponseEntity<Void>` + `noContent().build()` |

### 2.3 DELETE 패턴

```java
@DeleteMapping("/me")
public ResponseEntity<Void> withdraw(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        HttpServletRequest request) {

    validatePrincipal(principal);
    withdrawalService.withdraw(principal.getUserId(), provider, accessToken);
    return ResponseEntity.noContent().build();
}
```

---

## 3. DTO 패턴

### 3.1 Request DTO (입력)

```java
@Schema(description = "프로필 수정 요청")
public record UpdateProfileRequest(
        @Schema(description = "이름", example = "홍길동")
        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 100, message = "이름은 100자 이하여야 합니다")
        String name,

        @Schema(description = "전화번호", example = "010-1234-5678")
        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
        String phone,

        @Schema(description = "프로필 이미지 URL")
        @Size(max = 500)
        String profileImageUrl
) {
}
```

### 3.2 Command DTO (Service 입력용)

Controller와 Service 사이의 데이터 전달용. Request에서 변환하여 사용.

```java
public record UpdateProfileCommand(
        String name,
        String phone,
        String profileImageUrl
) {
    public static UpdateProfileCommand from(UpdateProfileRequest request) {
        return new UpdateProfileCommand(
                request.name(),
                request.phone(),
                request.profileImageUrl()
        );
    }
}
```

**Controller에서 사용:**

```java
return ApiResponse.success(
    userService.updateProfile(principal.getUserId(), UpdateProfileCommand.from(request))
);
```

### 3.3 Response DTO (출력)

```java
@Schema(description = "사용자 프로필 응답")
public record UserProfileResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "이메일", example = "user@kakao.com")
        String email,

        @Schema(description = "이름", example = "홍길동")
        String name
        // ... 기타 필드
) {
    // Entity → Response 변환 정적 팩토리
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName()
        );
    }
}
```

### 3.4 DTO 네이밍 규칙

| 용도 | 네이밍 | 예시 |
|------|--------|------|
| 요청 | `Xxx**Request**` | `UpdateProfileRequest` |
| 응답 | `Xxx**Response**` | `UserProfileResponse` |
| 서비스 입력 | `Xxx**Command**` | `UpdateProfileCommand` |

---

## 4. Service 패턴

### 4.1 기본 구조

```java
@Transactional(readOnly = true)  // 클래스 레벨: 기본 읽기 전용
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    // 조회: 기본 트랜잭션 (readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = findActiveUser(userId);
        return UserProfileResponse.from(user);
    }

    // 수정: 메서드 레벨 트랜잭션 오버라이드
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileCommand command) {
        User user = findActiveUser(userId);
        user.updateProfile(command.name(), command.phone(), command.profileImageUrl());
        return UserProfileResponse.from(user);
    }

    // 공통 조회 메서드 (soft delete 대응)
    private User findActiveUser(Long userId) {
        return userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED)
                .orElseThrow(() -> UserException.notFound(userId));
    }
}
```

### 4.2 핵심 규칙

| 항목 | 규칙 |
|------|------|
| 클래스 트랜잭션 | `@Transactional(readOnly = true)` |
| 수정 메서드 | `@Transactional` 명시 |
| 반환 타입 | Response DTO (Entity 반환 금지) |
| 예외 | Static factory 사용 (`XxxException.notFound()`) |
| Soft Delete | `findByIdAndStatusNot(id, DELETED)` 패턴 |

### 4.3 서비스 분리 기준

| 서비스 | 책임 |
|--------|------|
| `UserService` | 프로필 CRUD |
| `WithdrawalService` | 계정 탈퇴 전담 (OAuth unlink 포함) |

복잡한 비즈니스 로직은 별도 서비스로 분리.

---

## 5. Exception 패턴

### 5.1 ErrorCode enum

```java
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다"),
    PROFILE_UPDATE_FAILED(HttpStatus.BAD_REQUEST, "USER_002", "프로필 업데이트에 실패했습니다"),
    ALREADY_DELETED(HttpStatus.BAD_REQUEST, "USER_003", "이미 탈퇴한 사용자입니다"),
    ADMIN_CANNOT_WITHDRAW(HttpStatus.BAD_REQUEST, "USER_004", "관리자는 일반 탈퇴할 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

**코드 네이밍**: `{DOMAIN}_{3자리숫자}` (예: `USER_001`, `QUIZ_003`, `REC_101`)

### 5.2 Exception 클래스 (Static Factory)

```java
public class UserException extends BusinessException {

    // private 생성자
    private UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    private UserException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    // Static Factory 메서드
    public static UserException notFound() {
        return new UserException(UserErrorCode.USER_NOT_FOUND);
    }

    public static UserException notFound(Long userId) {
        return new UserException(UserErrorCode.USER_NOT_FOUND,
                "사용자를 찾을 수 없습니다: " + userId);
    }

    public static UserException alreadyDeleted() {
        return new UserException(UserErrorCode.ALREADY_DELETED);
    }
}
```

**사용:**

```java
// Good
throw UserException.notFound(userId);
throw QuizException.sessionNotFound();

// Bad (직접 생성자 호출)
throw new UserException(UserErrorCode.USER_NOT_FOUND);
```

### 5.3 GlobalExceptionHandler

모든 예외는 `ApiResponse` 형식으로 변환되어 반환됨.

```json
{
  "success": false,
  "error": {
    "code": "USER_001",
    "message": "사용자를 찾을 수 없습니다: 999"
  }
}
```

---

## 6. Entity 패턴

### 6.1 기본 구조

```java
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA용 기본 생성자
@Entity
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    // Builder 패턴 (필수 필드만)
    @Builder
    public User(String email, String name, ...) {
        this.email = email;
        this.name = name;
        // ...
    }

    // 비즈니스 메서드 (상태 변경)
    public void updateProfile(String name, String phone, String profileImageUrl) {
        this.name = name;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
    }

    public void delete() {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    // 상태 확인 메서드
    public boolean isDeleted() {
        return this.status == UserStatus.DELETED;
    }
}
```

### 6.2 BaseEntity

```java
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
```

### 6.3 핵심 규칙

| 항목 | 규칙 |
|------|------|
| 기본 생성자 | `@NoArgsConstructor(access = PROTECTED)` |
| ID 전략 | `GenerationType.IDENTITY` |
| Enum 저장 | `@Enumerated(EnumType.STRING)` |
| Soft Delete | `status` 필드 + `deletedAt` 타임스탬프 |
| 시간 필드 | `BaseEntity` 상속 (createdAt, modifiedAt 자동) |

---

## 7. Repository 패턴

### 7.1 기본 구조

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // Soft Delete 대응 조회
    Optional<User> findByIdAndStatusNot(Long id, UserStatus status);

    // 단순 조회
    Optional<User> findByEmail(String email);

    // 존재 여부
    boolean existsByEmail(String email);

    // 복합 조건
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}
```

### 7.2 네이밍 규칙

| 패턴 | 예시 |
|------|------|
| 단건 조회 | `findByXxx` → `Optional<T>` |
| 목록 조회 | `findAllByXxx` → `List<T>` |
| 존재 확인 | `existsByXxx` → `boolean` |
| Soft Delete | `findByIdAndStatusNot(id, DELETED)` |

### 7.3 커스텀 쿼리 (필요 시)

```java
@Query("SELECT u FROM User u WHERE u.status = :status AND u.createdAt > :date")
List<User> findRecentActiveUsers(@Param("status") UserStatus status,
                                  @Param("date") LocalDateTime date);
```

---

## 8. Test 패턴

### 8.1 단위 테스트 구조

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@kakao.com")
                .name("tester")
                .build();
    }

    @Nested
    @DisplayName("내 프로필 조회")
    class GetMyProfile {

        @Test
        @DisplayName("성공 - 유저 정보를 반환한다")
        void success() {
            // given
            Long userId = 1L;
            given(userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED))
                    .willReturn(Optional.of(user));

            // when
            UserProfileResponse result = userService.getMyProfile(userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.email()).isEqualTo("test@kakao.com");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 유저")
        void fail_userNotFound() {
            // given
            Long userId = 999L;
            given(userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getMyProfile(userId))
                    .isInstanceOf(UserException.class);
        }
    }
}
```

### 8.2 테스트 규칙

| 항목 | 규칙 |
|------|------|
| 구조 | `@Nested` + `@DisplayName` |
| 패턴 | given-when-then |
| Mock | `@Mock` + `@InjectMocks` |
| 검증 | AssertJ (`assertThat`, `assertThatThrownBy`) |
| 네이밍 | `success`, `fail_{실패사유}` |

---

## 9. Swagger 문서화 패턴

### 9.1 Controller 문서화

```java
@Operation(
    summary = "세션 완료 처리",
    description = """
        퀴즈 세션을 완료 처리합니다.

        **에러 코드:**
        | 코드 | HTTP | 설명 |
        |------|------|------|
        | QUIZ_003 | 404 | 퀴즈 세션을 찾을 수 없습니다 |
        | QUIZ_004 | 400 | 이미 완료된 퀴즈 세션입니다 |
        """
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "성공"),
    @ApiResponse(responseCode = "400", description = "검증 실패 (QUIZ_004)"),
    @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음 (QUIZ_003)")
})
```

### 9.2 DTO 문서화

```java
@Schema(description = "사용자 프로필 응답")
public record UserProfileResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "이메일", example = "user@kakao.com")
        String email
) { }
```

---

## 10. 요약: 코드 작성 체크리스트

### Controller
- [ ] `ApiResponse<T>` 래퍼 사용
- [ ] `@Operation` + `@ApiResponses` 문서화
- [ ] DELETE는 `ResponseEntity<Void>` 반환

### Service
- [ ] 클래스에 `@Transactional(readOnly = true)`
- [ ] 수정 메서드에 `@Transactional`
- [ ] Response DTO 반환 (Entity 반환 금지)

### DTO
- [ ] Java `record` 사용
- [ ] Request: validation 어노테이션
- [ ] Response: `from()` 정적 팩토리

### Exception
- [ ] ErrorCode: `{DOMAIN}_{숫자}` 형식
- [ ] Exception: private 생성자 + static factory

### Entity
- [ ] `BaseEntity` 상속
- [ ] `@NoArgsConstructor(access = PROTECTED)`
- [ ] Soft delete용 status 필드

### Test
- [ ] `@Nested` + `@DisplayName`
- [ ] given-when-then 패턴
- [ ] 성공/실패 케이스 분리
