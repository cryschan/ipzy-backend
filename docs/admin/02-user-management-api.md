# 회원관리 API 설계

> 작성일: 2024-12-18
> 상태: 구현 완료

## 1. 개요

### 1.1 목적
관리자가 회원을 조회하고 관리할 수 있는 API 구현

### 1.2 요구사항

| 항목 | 내용 |
|------|------|
| 조회 권한 | ADMIN, SUPER_ADMIN만 가능 |
| 검색 조건 | 이메일, 이름, 상태, 역할, 가입일 |
| 상태 변경 | ACTIVE, SUSPENDED, DELETED |

### API 엔드포인트
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/admin/users` | 회원 목록 (페이징, 검색) |
| GET | `/api/admin/users/{userId}` | 회원 상세 |
| PATCH | `/api/admin/users/{userId}/status` | 회원 상태 변경 |

---

## 2. 기술 결정 사항

### 2.1 동적 쿼리 방식: JPA Specification

**선택 이유:**
- 현재 프로젝트에 QueryDSL 미사용
- 추가 의존성/설정 없이 빠른 구현 가능
- 회원 검색은 비교적 단순한 조건 (keyword, status, role, dateRange)

**구현 방식:**
```java
public class AdminUserSpecification {
    public static Specification<User> withConditions(AdminUserSearchRequest request) {
        return Specification
            .where(emailOrNameContains(request.keyword()))
            .and(statusEquals(request.status()))
            .and(roleEquals(request.role()))
            .and(createdAtBetween(request.createdFrom(), request.createdTo()));
    }
}
```

### 2.2 구독 정보 범위: 최신 구독만

**선택 이유:**
- MVP 단계에서 빠른 구현 우선
- 구독 히스토리는 별도 API로 분리 가능
- 응답 크기 최소화

**응답 구조:**
```json
{
  "subscription": {
    "planName": "PRO",
    "status": "ACTIVE",
    "startDate": "2024-02-01T00:00:00",
    "endDate": "2024-03-01T00:00:00"
  }
}
```

**사용 메서드:**
```java
subscriptionRepository.findTopByUserOrderByCreatedAtDesc(user)
```

### 2.3 감사 로그: TODO

**상태:** 추후 구현 예정
- 현재는 로그 레벨로만 기록
- `01-activity-logging-system.md` 참고하여 구현 예정

---

## 3. 파일 구조

```
src/main/java/com/ipzy/domain/admin/
├── controller/
│   └── AdminUserController.java
├── dto/
│   ├── AdminUserResponse.java           (목록용)
│   ├── AdminUserDetailResponse.java     (상세용)
│   ├── AdminUserSearchRequest.java      (검색 조건)
│   └── AdminUserStatusChangeRequest.java(상태 변경)
├── service/
│   └── AdminUserService.java
└── specification/
    └── AdminUserSpecification.java
```

---

## 4. 상세 설계

### 4.1 DTO 설계

#### AdminUserResponse (목록용)
```java
public record AdminUserResponse(
    Long id,
    String email,
    String name,
    UserStatus status,
    UserRole role,
    String provider,          // KAKAO, GOOGLE 등
    String planName,          // 구독 플랜명 (간략)
    LocalDateTime createdAt
) {
    public static AdminUserResponse from(User user, Subscription subscription) { ... }
}
```

#### AdminUserDetailResponse (상세용)
```java
public record AdminUserDetailResponse(
    Long id,
    String email,
    String name,
    UserStatus status,
    UserRole role,
    String provider,
    String providerId,
    LocalDateTime createdAt,
    LocalDateTime lastLoginAt,
    SubscriptionSummary subscription
) {
    public record SubscriptionSummary(
        Long id,
        String planName,
        String displayName,
        Integer price,
        SubscriptionStatus status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Boolean autoRenew
    ) {
        public static SubscriptionSummary from(Subscription subscription) { ... }
    }
}
```

#### AdminUserSearchRequest (검색 조건)
```java
public record AdminUserSearchRequest(
    String keyword,           // 이메일, 이름 검색
    UserStatus status,        // ACTIVE, SUSPENDED, DORMANT, WITHDRAWN
    UserRole role,            // USER, ADMIN
    LocalDate createdFrom,    // 가입일 시작
    LocalDate createdTo,      // 가입일 종료
    @Min(0) int page,
    @Min(1) @Max(100) int size
) {
    public AdminUserSearchRequest {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
    }
}
```

#### AdminUserStatusChangeRequest (상태 변경)
```java
public record AdminUserStatusChangeRequest(
    @NotNull UserStatus status,
    @Size(max = 500) String reason
) {}
```

### 4.2 Specification 설계

```java
public class AdminUserSpecification {

    public static Specification<User> emailOrNameContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("name")), pattern)
            );
        };
    }

    public static Specification<User> statusEquals(UserStatus status) {
        return (root, query, cb) ->
            status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<User> roleEquals(UserRole role) {
        return (root, query, cb) ->
            role == null ? null : cb.equal(root.get("role"), role);
    }

    public static Specification<User> createdAtBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"),
                    from.atStartOfDay(), to.plusDays(1).atStartOfDay());
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay());
            }
            return cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay());
        };
    }

    public static Specification<User> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }
}
```

### 4.3 Service 설계

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    public Page<AdminUserResponse> findUsers(AdminUserSearchRequest request) {
        Specification<User> spec = Specification
            .where(AdminUserSpecification.notDeleted())
            .and(AdminUserSpecification.emailOrNameContains(request.keyword()))
            .and(AdminUserSpecification.statusEquals(request.status()))
            .and(AdminUserSpecification.roleEquals(request.role()))
            .and(AdminUserSpecification.createdAtBetween(
                request.createdFrom(), request.createdTo()));

        Pageable pageable = PageRequest.of(request.page(), request.size(),
            Sort.by(Sort.Direction.DESC, "createdAt"));

        return userRepository.findAll(spec, pageable)
            .map(user -> AdminUserResponse.from(user,
                subscriptionRepository.findTopByUserOrderByCreatedAtDesc(user).orElse(null)));
    }

    public AdminUserDetailResponse findUserDetail(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(AdminException::userNotFound);
        Subscription subscription = subscriptionRepository
            .findTopByUserOrderByCreatedAtDesc(user).orElse(null);
        return AdminUserDetailResponse.from(user, subscription);
    }

    @Transactional
    public AdminUserDetailResponse changeUserStatus(Long userId,
            AdminUserStatusChangeRequest request, Long adminId) {
        User user = userRepository.findByIdForUpdate(userId)
            .orElseThrow(AdminException::userNotFound);

        UserStatus beforeStatus = user.getStatus();
        user.changeStatus(request.status());

        // TODO: 감사 로그 기록 기능 추가 예정
        log.info("회원 상태 변경: userId={}, {} -> {}, adminId={}",
            userId, beforeStatus, request.status(), adminId);

        return AdminUserDetailResponse.from(user,
            subscriptionRepository.findTopByUserOrderByCreatedAtDesc(user).orElse(null));
    }
}
```

### 4.4 Controller 설계

```java
@Tag(name = "Admin User", description = "관리자용 회원 관리 API")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminAuthService adminAuthService;

    // TODO: Interceptor로 관리자 인증 공통 처리 필요

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(
            @ParameterObject @Valid AdminUserSearchRequest request,
            HttpSession session) {
        validateAdminSession(session);
        return ResponseEntity.ok(ApiResponse.success(adminUserService.findUsers(request)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getUserDetail(
            @PathVariable Long userId, HttpSession session) {
        validateAdminSession(session);
        return ResponseEntity.ok(ApiResponse.success(adminUserService.findUserDetail(userId)));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> changeUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusChangeRequest request,
            HttpSession session) {
        Long adminId = validateAdminSession(session);
        return ResponseEntity.ok(ApiResponse.success(
            adminUserService.changeUserStatus(userId, request, adminId)));
    }

    private Long validateAdminSession(HttpSession session) {
        Long adminId = adminAuthService.getAdminIdFromSession(session);
        if (adminId == null) throw AdminException.sessionRequired();
        return adminId;
    }
}
```

---

## 5. 구현 순서

| 순서 | 작업 | 상태 |
|------|------|:----:|
| 1 | DTO 4개 생성 | ✅ |
| 2 | AdminUserSpecification 생성 | ✅ |
| 3 | AdminUserService 생성 | ✅ |
| 4 | AdminUserController 생성 | ✅ |
| 5 | User 엔티티에 changeStatus 메서드 추가 | ✅ |
| 6 | AdminException에 userNotFound 추가 | ✅ |
| 7 | 빌드 및 테스트 | ✅ |

---

## 6. TODO

| 항목 | 설명 | 우선순위 |
|------|------|:--------:|
| N+1 쿼리 해결 | 회원 목록 조회 시 구독 Batch 조회 | P0 |
| Interceptor 적용 | 관리자 인증 공통 처리 | P1 |
| 감사 로그 | 상태 변경 이력 기록 | P1 |
| 상태 전이 검증 | DELETED → ACTIVE 등 검증 | P2 |
