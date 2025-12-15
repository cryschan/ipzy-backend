# 유저 활동 로깅 시스템 설계

> 작성일: 2024-12-15
> 상태: 설계 완료, 구현 예정

## 1. 개요

### 1.1 목적
사용자의 모든 활동을 기록하여 서비스 분석 및 관리자 모니터링을 지원하는 시스템

### 1.2 요구사항

| 항목 | 내용 |
|------|------|
| 로깅 범위 | 전 활동 (모든 API 호출) |
| 조회 권한 | ADMIN, SUPER_ADMIN만 가능 |
| 본인 활동 조회 | 불가 (Admin이 자신의 로그 조회 불가) |
| 데이터 보존 | 90일 (자동 삭제) |

---

## 2. 아키텍처

### 2.1 시스템 흐름도

```
┌─────────────────────────────────────────────────────────────────────┐
│                         HTTP Request                                 │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  ActivityLoggingInterceptor                          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  preHandle()                                                 │    │
│  │  - 요청 시작 시간 기록                                         │    │
│  │  - 요청 정보 캡처 (URI, Method, IP, User-Agent)               │    │
│  └─────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Controller 처리                                 │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  ActivityLoggingInterceptor                          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  afterCompletion()                                           │    │
│  │  - ActivityLogEvent 발행 (Spring Event)                      │    │
│  └─────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      HTTP Response 반환                              │
└─────────────────────────────────────────────────────────────────────┘

        ┌───────────────────────────────────────────────────────┐
        │                비동기 처리 (별도 스레드)                  │
        └───────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│              ActivityLogEventListener (@Async)                       │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  - ActivityLogEvent 수신                                     │    │
│  │  - ActivityLog 엔티티 생성                                    │    │
│  │  - DB 저장                                                   │    │
│  └─────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    PostgreSQL (activity_logs)                        │
└─────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│              ActivityLogCleanupScheduler                             │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  @Scheduled(cron = "0 0 3 * * *")  // 매일 03:00             │    │
│  │  - 90일 이전 로그 배치 삭제                                    │    │
│  └─────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 컴포넌트 구성

```
┌─────────────────────────────────────────────────────────────────────┐
│                        _global 레이어                                │
├─────────────────────────────────────────────────────────────────────┤
│  config/                                                            │
│    ├── AsyncConfig.java          # @EnableAsync + ThreadPool        │
│    ├── SchedulingConfig.java     # @EnableScheduling                │
│    └── WebConfig.java            # Interceptor 등록 (수정)           │
│                                                                     │
│  interceptor/                                                       │
│    ├── ActivityLoggingInterceptor.java   # 요청 캡처                │
│    └── LogActivity.java                  # 세부 타입 어노테이션      │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                      domain/activity 레이어                          │
├─────────────────────────────────────────────────────────────────────┤
│  entity/                                                            │
│    └── ActivityLog.java          # 기존 엔티티 (변경 없음)           │
│                                                                     │
│  repository/                                                        │
│    └── ActivityLogRepository.java    # JPA Repository               │
│                                                                     │
│  event/                                                             │
│    ├── ActivityLogEvent.java         # 이벤트 객체                  │
│    └── ActivityLogEventListener.java # 비동기 리스너                │
│                                                                     │
│  service/                                                           │
│    └── ActivityLogService.java       # 조회 서비스                  │
│                                                                     │
│  controller/                                                        │
│    └── ActivityLogController.java    # Admin API                    │
│                                                                     │
│  dto/                                                               │
│    ├── ActivityLogResponse.java      # 응답 DTO                     │
│    └── ActivityLogSearchRequest.java # 검색 조건 DTO                │
│                                                                     │
│  scheduler/                                                         │
│    └── ActivityLogCleanupScheduler.java  # 90일 삭제                │
│                                                                     │
│  exception/                                                         │
│    ├── ActivityErrorCode.java        # 에러 코드                    │
│    └── ActivityException.java        # 예외 클래스                  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. 상세 설계

### 3.1 ActivityType 확장

기존 enum에 다음 타입 추가:

```java
public enum ActivityType {
    // === 기존 (유지) ===
    LOGIN,                  // 로그인
    LOGOUT,                 // 로그아웃
    QUIZ_START,             // 퀴즈 시작
    QUIZ_COMPLETE,          // 퀴즈 완료
    RECOMMENDATION_VIEW,    // 추천 코디 조회
    RECOMMENDATION_SAVE,    // 추천 코디 저장
    PRODUCT_VIEW,           // 상품 상세 조회
    PRODUCT_CLICK,          // 상품 외부 링크 클릭
    SUBSCRIPTION_START,     // 구독 시작
    SUBSCRIPTION_CANCEL,    // 구독 취소
    PROFILE_UPDATE,         // 프로필 수정
    PASSWORD_CHANGE,        // 비밀번호 변경

    // === 신규 추가 ===
    API_REQUEST,            // 일반 API 호출 (기본값)
    ADMIN_ACTIVITY_LOG_VIEW // Admin 활동 로그 조회
}
```

### 3.2 Interceptor 설계

#### 제외 경로
```java
private static final List<String> EXCLUDE_PATTERNS = List.of(
    "/swagger-ui/**",       // Swagger UI
    "/v3/api-docs/**",      // OpenAPI 문서
    "/actuator/**",         // 헬스체크
    "/favicon.ico",         // 파비콘
    "/error"                // 에러 페이지
);
```

#### 세부 타입 지정 어노테이션
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogActivity {
    ActivityType value();
}

// 사용 예시
@LogActivity(ActivityType.PROFILE_UPDATE)
@PutMapping("/me")
public ApiResponse<UserProfileResponse> updateProfile(...) { }
```

### 3.3 Admin API 설계

#### 엔드포인트
```
GET /api/admin/activity-logs
```

#### 요청 파라미터
| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| userId | Long | X | 특정 유저 필터 | 123 |
| activityType | String | X | 활동 타입 필터 | LOGIN |
| startDate | LocalDate | X | 시작일 | 2024-01-01 |
| endDate | LocalDate | X | 종료일 | 2024-01-31 |
| page | int | X | 페이지 번호 (기본: 0) | 0 |
| size | int | X | 페이지 크기 (기본: 20, 최대: 100) | 20 |

#### 응답 예시
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "userId": 123,
        "userName": "홍길동",
        "userEmail": "hong@example.com",
        "activityType": "LOGIN",
        "metadata": {
          "uri": "/api/auth/me",
          "method": "GET",
          "statusCode": 200
        },
        "ipAddress": "192.168.1.1",
        "userAgent": "Mozilla/5.0...",
        "createdAt": "2024-12-15T10:30:00"
      }
    ],
    "pagination": {
      "currentPage": 1,
      "totalPages": 10,
      "totalItems": 195,
      "itemsPerPage": 20,
      "hasNext": true,
      "hasPrev": false
    }
  }
}
```

### 3.4 본인 조회 차단 로직

```java
@GetMapping
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public ApiResponse<PageResponse<ActivityLogResponse>> getActivityLogs(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        ActivityLogSearchRequest request,
        Pageable pageable) {

    Long adminUserId = principal.getUserId();

    // 1. 명시적 본인 조회 시도 차단
    if (request.getUserId() != null && request.getUserId().equals(adminUserId)) {
        throw ActivityException.cannotViewOwnLogs();
    }

    // 2. 검색 결과에서 항상 본인 로그 제외
    Page<ActivityLog> logs = activityLogService.search(request, adminUserId, pageable);

    return ApiResponse.success(PageResponse.from(logs.map(ActivityLogResponse::from)));
}
```

### 3.5 90일 자동 삭제 스케줄러

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityLogCleanupScheduler {

    private final ActivityLogRepository activityLogRepository;
    private static final int RETENTION_DAYS = 90;
    private static final int BATCH_SIZE = 1000;

    @Scheduled(cron = "0 0 3 * * *")  // 매일 03:00
    @Transactional
    public void cleanupOldActivityLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(RETENTION_DAYS);

        log.info("활동 로그 정리 시작. 기준일: {}", cutoffDate);

        int totalDeleted = 0;
        int deleted;

        do {
            deleted = activityLogRepository.deleteOldLogsBatch(cutoffDate, BATCH_SIZE);
            totalDeleted += deleted;

            if (deleted == BATCH_SIZE) {
                // 배치 간 100ms 대기 (DB 부하 분산)
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } while (deleted == BATCH_SIZE);

        log.info("활동 로그 정리 완료. 삭제 건수: {}", totalDeleted);
    }
}
```

### 3.6 비동기 설정

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);        // 기본 스레드 수
        executor.setMaxPoolSize(5);         // 최대 스레드 수
        executor.setQueueCapacity(100);     // 대기 큐 크기
        executor.setThreadNamePrefix("activity-log-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("비동기 로깅 실패 [{}]: {}", method.getName(), ex.getMessage());
        };
    }
}
```

---

## 4. 데이터베이스

### 4.1 기존 테이블 (activity_logs)

```sql
CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    activity_type VARCHAR(30) NOT NULL,
    metadata JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 인덱스 (기존)
CREATE INDEX idx_activity_user_id ON activity_logs(user_id);
CREATE INDEX idx_activity_type ON activity_logs(activity_type);
CREATE INDEX idx_activity_created_at ON activity_logs(created_at);
```

### 4.2 metadata 저장 예시

```json
{
  "uri": "/api/users/me",
  "method": "PUT",
  "statusCode": 200,
  "responseTime": 45,
  "requestParams": {
    "name": "홍길동"
  }
}
```

### 4.3 민감정보 필터링

metadata에 저장하지 않는 필드:
- password, pwd
- token, accessToken, refreshToken
- secret, apiKey
- Authorization 헤더

---

## 5. 보안 고려사항

### 5.1 권한 설정

```java
// SecurityConfig.java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
    // 기존 설정 유지
)
```

### 5.2 에러 코드

| 코드 | HTTP Status | 메시지 |
|------|-------------|--------|
| ACTIVITY_001 | 403 FORBIDDEN | 본인의 활동 로그는 조회할 수 없습니다 |
| ACTIVITY_002 | 400 BAD_REQUEST | 유효하지 않은 검색 조건입니다 |

---

## 6. 구현 순서

### Phase 1: 기반 설정
1. `ActivityType` enum 확장
2. `ActivityErrorCode`, `ActivityException` 생성
3. `AsyncConfig` 생성
4. `SchedulingConfig` 생성

### Phase 2: 핵심 로깅 기능
5. `ActivityLogRepository` 생성
6. `ActivityLogEvent` 생성
7. `ActivityLogEventListener` 생성
8. `LogActivity` 어노테이션 생성
9. `ActivityLoggingInterceptor` 생성
10. `WebConfig` 수정

### Phase 3: Admin 조회 API
11. `ActivityLogSearchRequest` DTO 생성
12. `ActivityLogResponse` DTO 생성
13. `ActivityLogService` 생성
14. `ActivityLogController` 생성
15. `SecurityConfig` 수정

### Phase 4: 자동 정리
16. `ActivityLogCleanupScheduler` 생성

---

## 7. 테스트 계획

### 7.1 단위 테스트
- [ ] ActivityLogService 조회 로직 테스트
- [ ] ActivityLogRepository 쿼리 테스트
- [ ] 본인 조회 차단 로직 테스트

### 7.2 통합 테스트
- [ ] Interceptor → Event → Listener → DB 저장 플로우
- [ ] Admin API 권한 테스트
- [ ] 90일 삭제 스케줄러 테스트

---

## 8. 참고 자료

### 관련 파일
- `domain/activity/entity/ActivityLog.java` - 기존 엔티티
- `domain/user/exception/UserException.java` - Exception 패턴 참조
- `_global/common/PageResponse.java` - 페이징 응답 패턴
- `_global/common/ApiResponse.java` - 공통 응답 래퍼

### 의존성
- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL + JSONB
- Hypersistence Utils (JsonType)
