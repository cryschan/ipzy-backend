# 실전 예제 모음

## 1. 코드 리뷰어

**파일**: `.claude/skills/code-reviewer/SKILL.md`

```yaml
---
name: code-reviewer
description: 코드 품질, 보안, 성능 검토. PR 리뷰, 코드 검토, 품질 분석 시 사용.
allowed-tools: Read, Grep, Glob
---

# 코드 리뷰어

## 체크리스트

### 1. 코드 품질
- [ ] 단일 책임 원칙 준수
- [ ] 중복 코드 없음
- [ ] 명확한 변수/함수명

### 2. 보안
- [ ] SQL 인젝션 방지
- [ ] XSS 방지
- [ ] 민감 정보 노출 없음

### 3. 성능
- [ ] N+1 쿼리 없음
- [ ] 불필요한 렌더링 없음
- [ ] 메모리 누수 없음

## Output Format
```markdown
## 리뷰 결과

### 잘된 점
- ...

### 개선 필요
- 파일:라인 - 문제 설명

### 필수 수정
- 파일:라인 - 심각한 문제
```
```

---

## 2. Spring Boot API 생성기

**파일**: `.claude/skills/spring-api-generator/SKILL.md`

```yaml
---
name: spring-api-generator
description: Spring Boot REST API 엔드포인트 생성. API 추가, Controller 생성 시 사용.
---

# Spring Boot API 생성기

## 구조
```
src/main/java/com/example/
├── controller/
│   └── ResourceController.java
├── service/
│   └── ResourceService.java
├── repository/
│   └── ResourceRepository.java
├── domain/
│   └── Resource.java
└── dto/
    ├── ResourceRequest.java
    └── ResourceResponse.java
```

## Controller 템플릿
```java
@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<List<ResourceResponse>> findAll() {
        return ResponseEntity.ok(resourceService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ResourceResponse> create(
            @Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResourceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.ok(resourceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

## Service 템플릿
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public List<ResourceResponse> findAll() {
        return resourceRepository.findAll().stream()
                .map(ResourceResponse::from)
                .toList();
    }

    public ResourceResponse findById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
        return ResourceResponse.from(resource);
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource resource = request.toEntity();
        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
        resource.update(request);
        return ResourceResponse.from(resource);
    }

    @Transactional
    public void delete(Long id) {
        if (!resourceRepository.existsById(id)) {
            throw new ResourceNotFoundException(id);
        }
        resourceRepository.deleteById(id);
    }
}
```
```

---

## 3. 테스트 코드 생성기

**파일**: `.claude/skills/test-writer/SKILL.md`

```yaml
---
name: test-writer
description: JUnit/Mockito 테스트 코드 생성. 테스트 작성, 유닛 테스트, 커버리지 향상 시 사용.
---

# 테스트 코드 생성기

## 테스트 패턴

### Service 테스트
```java
@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ResourceService resourceService;

    @Test
    @DisplayName("ID로 리소스 조회 - 성공")
    void findById_Success() {
        // given
        Long id = 1L;
        Resource resource = createResource(id);
        given(resourceRepository.findById(id)).willReturn(Optional.of(resource));

        // when
        ResourceResponse result = resourceService.findById(id);

        // then
        assertThat(result.getId()).isEqualTo(id);
        then(resourceRepository).should().findById(id);
    }

    @Test
    @DisplayName("ID로 리소스 조회 - 실패 (존재하지 않음)")
    void findById_NotFound() {
        // given
        Long id = 999L;
        given(resourceRepository.findById(id)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> resourceService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

### Controller 테스트
```java
@WebMvcTest(ResourceController.class)
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResourceService resourceService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/resources/{id} - 성공")
    void findById_Success() throws Exception {
        // given
        Long id = 1L;
        ResourceResponse response = createResourceResponse(id);
        given(resourceService.findById(id)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/resources/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    @DisplayName("POST /api/v1/resources - 성공")
    void create_Success() throws Exception {
        // given
        ResourceRequest request = createResourceRequest();
        ResourceResponse response = createResourceResponse(1L);
        given(resourceService.create(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }
}
```

## 커버리지 목표
- Line Coverage: 80%+
- Branch Coverage: 75%+
- Method Coverage: 80%+
```

---

## 4. Git 워크플로우 자동화

**파일**: `~/.claude/skills/git-workflow/SKILL.md`

```yaml
---
name: git-workflow
description: Git 브랜치 생성, 커밋, PR 자동화. 브랜치 관리, PR 생성 시 사용.
---

# Git 워크플로우

## 브랜치 네이밍
```
feature/[이슈번호]-간단설명
fix/[이슈번호]-버그설명
hotfix/긴급수정
refactor/리팩토링설명
```

## 커밋 메시지 컨벤션
```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types
| Type | 설명 |
|------|------|
| feat | 새 기능 |
| fix | 버그 수정 |
| docs | 문서 |
| style | 포맷팅 |
| refactor | 리팩토링 |
| test | 테스트 |
| chore | 기타 |

## PR 템플릿
```markdown
## 변경 사항
-

## 관련 이슈
- Closes #이슈번호

## 체크리스트
- [ ] 테스트 통과
- [ ] 문서 업데이트
- [ ] 코드 리뷰 준비

## 스크린샷 (UI 변경시)
```
```

---

## 5. 에러 핸들링 패턴

**파일**: `.claude/skills/error-handling/SKILL.md`

```yaml
---
name: error-handler
description: Spring Boot 에러 처리 패턴 적용. 예외 처리, 에러 핸들링 구현 시 사용.
---

# 에러 핸들링 패턴

## 커스텀 예외 클래스
```java
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(Long id) {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
```

## ErrorCode Enum
```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(400, "C001", "잘못된 입력값입니다"),
    INTERNAL_SERVER_ERROR(500, "C002", "서버 오류가 발생했습니다"),

    // Resource
    RESOURCE_NOT_FOUND(404, "R001", "리소스를 찾을 수 없습니다"),
    RESOURCE_ALREADY_EXISTS(409, "R002", "이미 존재하는 리소스입니다");

    private final int status;
    private final String code;
    private final String message;
}
```

## Global Exception Handler
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.error("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e) {
        log.error("ValidationException: {}", e.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error: ", e);
        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
```

## ErrorResponse DTO
```java
@Getter
@Builder
public class ErrorResponse {

    private final int status;
    private final String code;
    private final String message;
    private final List<FieldError> errors;

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .status(errorCode.getStatus())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .errors(Collections.emptyList())
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
        return ErrorResponse.builder()
                .status(errorCode.getStatus())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .errors(FieldError.of(bindingResult))
                .build();
    }

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String value;
        private final String reason;

        public static List<FieldError> of(BindingResult bindingResult) {
            return bindingResult.getFieldErrors().stream()
                    .map(error -> FieldError.builder()
                            .field(error.getField())
                            .value(error.getRejectedValue() != null ?
                                    error.getRejectedValue().toString() : "")
                            .reason(error.getDefaultMessage())
                            .build())
                    .toList();
        }
    }
}
```
```

---

## 6. API 문서 생성기

**파일**: `.claude/skills/api-doc-generator/SKILL.md`

```yaml
---
name: api-doc-generator
description: API 엔드포인트 문서 자동 생성. API 문서화, 엔드포인트 정리 시 사용.
---

# API 문서 생성기

## Instructions
1. Controller 파일 스캔
2. HTTP 메서드별 정리
3. 요청/응답 스키마 추출

## Output Template

### `[METHOD] /api/v1/경로`

**설명**: 엔드포인트 설명

**Request**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| id | Long | O | 사용자 ID |

**Request Body**
```json
{
  "name": "string",
  "email": "string"
}
```

**Response**
```json
{
  "id": 1,
  "name": "string",
  "createdAt": "2024-01-01T00:00:00"
}
```

**에러 코드**
| 코드 | 설명 |
|------|------|
| 400 | 잘못된 요청 |
| 401 | 인증 필요 |
| 404 | 리소스 없음 |
| 500 | 서버 오류 |
```

---

## 7. 데이터베이스 스키마 설계

**파일**: `.claude/skills/db-schema/SKILL.md`

```yaml
---
name: db-schema-designer
description: JPA Entity 스키마 설계. DB 모델링, Entity 작성 시 사용.
---

# DB 스키마 설계

## Entity 패턴

### 기본 Entity
```java
@Entity
@Table(name = "resources")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resource extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true)
    private String email;

    @Builder
    public Resource(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public void update(String name) {
        this.name = name;
    }
}
```

### BaseTimeEntity
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### 연관관계
```java
// 1:N 관계
@Entity
public class Post {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;
}

// N:M 관계
@Entity
public class Post {

    @ManyToMany
    @JoinTable(
        name = "post_tags",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();
}
```

## 체크리스트
- [ ] 적절한 ID 전략 (IDENTITY, SEQUENCE, UUID)
- [ ] BaseTimeEntity 상속 (createdAt, updatedAt)
- [ ] FetchType.LAZY 사용
- [ ] 연관관계 주인 설정
- [ ] Soft delete 고려 (deletedAt)
```

---

## 8. 환경변수 검증기

**파일**: `.claude/skills/env-validator/SKILL.md`

```yaml
---
name: env-validator
description: Spring Boot 환경변수 설정. application.yml 설정, 환경변수 관리 시 사용.
---

# 환경변수 설정

## application.yml 구조
```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/mydb}
    username: ${DATABASE_USERNAME:postgres}
    password: ${DATABASE_PASSWORD:password}

  jpa:
    hibernate:
      ddl-auto: ${JPA_DDL_AUTO:validate}
    properties:
      hibernate:
        format_sql: true
        default_batch_fetch_size: 100

jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:3600000}

logging:
  level:
    root: ${LOG_LEVEL:INFO}
    org.hibernate.SQL: DEBUG
```

## @ConfigurationProperties
```java
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Validated
@Getter
@Setter
public class JwtProperties {

    @NotBlank
    private String secret;

    @Positive
    private Long expiration;
}
```

## .env.example
```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/mydb
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=password

# JWT
JWT_SECRET=your-secret-key-min-32-characters-long
JWT_EXPIRATION=3600000

# Profile
SPRING_PROFILES_ACTIVE=local
```

## Profile별 설정
```
application.yml          # 공통 설정
application-local.yml    # 로컬 개발
application-dev.yml      # 개발 서버
application-prod.yml     # 운영 서버
```
```
