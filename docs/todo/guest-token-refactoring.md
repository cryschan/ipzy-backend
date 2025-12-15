# Guest Token 리팩토링 계획

> 작성일: 2025-12-12
> 관련 파일: `QuizSession.java`, `RecommendationService.java`

---

## 배경

### 현재 상태
- 비로그인 사용자가 퀴즈를 풀면 `QuizSession`의 `user_id=null`로 저장
- 클라이언트가 `sessionId`를 보관해야 회원가입 후 연동 가능
- `null` 체크가 여러 곳에 분산되어 있음

### 문제점
1. `sessionId` 노출 (DB PK 직접 노출)
2. `null` 기반 익명 판단 - 명시적이지 않음
3. 클라이언트가 세션 ID를 관리해야 함

### 목표
- `guestToken` (UUID) 기반으로 익명 세션 관리
- 보안성 향상 (세션 ID 대신 토큰 사용)
- 명시적인 익명 세션 식별

---

## 보안 고려사항

> `guestToken`은 **베어러 자격증명**처럼 동작합니다.
> 토큰 탈취 시 세션 접근 및 계정 연결이 가능하므로 보안 정책 필수.

### 저장 방식

| 방식 | 장점 | 단점 | 권장 |
|------|------|------|:----:|
| HttpOnly Cookie | XSS 방어, 자동 전송 | CSRF 대응 필요 | **권장** |
| localStorage | 구현 간단 | XSS 취약 | 비권장 |
| sessionStorage | 탭 격리 | 새 탭 시 재발급 | - |

### 보안 정책

| 항목 | 정책 |
|------|------|
| **만료** | 7일 또는 회원가입 시 폐기 |
| **1회성 연결** | `assignUser()` 호출 후 재연결 불가 |
| **로그 마스킹** | guestToken 로그 출력 금지 (앞 8자리만 허용) |
| **전송** | HTTPS 필수 |
| **Rate Limit** | 토큰 기반 요청 분당 60회 제한 (브루트포스 방지) |

### 구현 예시

```java
// 로그 마스킹
private String maskToken(String token) {
    if (token == null || token.length() < 8) return "****";
    return token.substring(0, 8) + "****";
}

log.info("세션 조회: guestToken={}", maskToken(guestToken));
```

---

## 영향 범위 분석

### 1. 엔티티 (2개) - 스키마 변경

| 파일 | 변경 내용 | 우선순위 |
|------|----------|----------|
| `QuizSession.java` | `guestToken` 컬럼 추가, `isAnonymous()` 로직 변경 | 높음 |
| `Recommendation.java` | `isAnonymous()` 참조 확인 | 중간 |

### 2. Repository (1개)

| 파일 | 변경 내용 | 우선순위 |
|------|----------|----------|
| `QuizSessionRepository.java` | `findByGuestToken(String)` 메서드 추가 | 높음 |

### 3. Service (2개)

| 파일 | 변경 내용 | 우선순위 |
|------|----------|----------|
| `QuizService.java:52-56` | 세션 생성 시 `guestToken` 생성 로직 추가 | 높음 |
| `RecommendationService.java:59-60` | `guestToken` 기반 세션 조회/연동 로직 변경 | 높음 |

### 4. DTO (1개)

| 파일 | 변경 내용 | 우선순위 |
|------|----------|----------|
| `QuizSessionStartResponse.java` | `guestToken` 필드 추가 | 높음 |

### 5. Controller (1개)

| 파일 | 변경 내용 | 우선순위 |
|------|----------|----------|
| `QuizController.java` | DTO 변경으로 자동 반영 | 낮음 |

### 6. 테스트 코드 (5개 이상)

| 파일 | 변경 내용 |
|------|----------|
| `RecommendationServiceTest.java` | `QuizSession.builder()` 호출부 수정 |
| `RecommendationTest.java` | `isAnonymous()` 테스트 수정 |
| `OutfitRecommendationDtoTest.java` | 익명 세션 테스트 수정 |
| `RecommendationRequestTest.java` | 세션 관련 테스트 확인 |
| `QuizAnswerDtoTest.java` | 세션 관련 테스트 확인 |

---

## 구현 상세

### 1. QuizSession 엔티티 변경

```java
@Entity
@Table(name = "quiz_sessions")
public class QuizSession extends BaseEntity {

    // 기존 필드들...

    // DDL은 Flyway로 단일 관리 (unique 제약은 여기서 선언하지 않음)
    @Column(name = "guest_token", length = 36)
    private String guestToken;  // 비로그인 시 UUID 저장

    @Builder
    public QuizSession(User user, Quiz quiz) {
        this.user = user;
        this.quiz = quiz;
        this.completed = false;
        // 비로그인 시 guestToken 생성
        if (user == null) {
            this.guestToken = UUID.randomUUID().toString();
        }
    }

    public boolean isAnonymous() {
        return this.guestToken != null && this.user == null;
    }

    // guestToken으로 생성된 세션에 사용자 연결
    public void assignUser(User user) {
        if (this.user != null) {
            throw new QuizException(QuizErrorCode.SESSION_ALREADY_ASSIGNED);
        }
        this.user = user;
        // guestToken은 유지 (히스토리 추적용) 또는 null 처리
    }
}
```

### 2. Repository 변경

```java
public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

    // 기존 메서드...

    Optional<QuizSession> findByGuestToken(String guestToken);

    @Query("""
        SELECT DISTINCT s FROM QuizSession s
        JOIN FETCH s.quiz q
        LEFT JOIN FETCH s.answers a
        LEFT JOIN FETCH a.question
        WHERE s.guestToken = :guestToken
    """)
    Optional<QuizSession> findByGuestTokenWithAnswers(String guestToken);
}
```

### 3. DTO 변경

```java
@Schema(description = "퀴즈 세션 시작 응답")
public class QuizSessionStartResponse {

    @Schema(description = "퀴즈 세션 ID", example = "1")
    private Long sessionId;

    @Schema(description = "게스트 토큰 (비로그인 시에만 반환)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String guestToken;  // 추가

    // 기존 필드들...

    public static QuizSessionStartResponse from(QuizSession session) {
        return new QuizSessionStartResponse(
                session.getId(),
                session.getGuestToken(),  // 추가
                session.getUser() != null ? session.getUser().getId() : null,
                session.getQuiz().getId(),
                session.getCompleted(),
                session.getCreatedAt()
        );
    }
}
```

### 4. DB 마이그레이션

> **DDL 소스 단일화**: 제약 조건은 Flyway에서만 관리 (엔티티 `@Column(unique=true)` 사용 금지)

```sql
-- V{version}__add_guest_token_to_quiz_sessions.sql

-- 1. 컬럼 추가
ALTER TABLE quiz_sessions ADD COLUMN guest_token VARCHAR(36);

-- 2. 유니크 인덱스 (null 허용, PostgreSQL 부분 인덱스)
CREATE UNIQUE INDEX idx_quiz_sessions_guest_token
ON quiz_sessions(guest_token)
WHERE guest_token IS NOT NULL;

-- 3. 기존 익명 세션 backfill (선택)
-- 방법 A: PostgreSQL 확장 사용 (pgcrypto 필요)
-- CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- UPDATE quiz_sessions SET guest_token = gen_random_uuid()::text WHERE user_id IS NULL;

-- 방법 B (권장): 앱에서 배치 처리
-- gen_random_uuid()는 pgcrypto 확장 필요, 환경에 따라 실패 가능
-- 앱에서 UUID.randomUUID().toString()으로 생성 권장
```

#### UUID 생성 전략

| 방법 | 장점 | 단점 | 권장 |
|------|------|------|:----:|
| 앱에서 생성 (`UUID.randomUUID()`) | 환경 독립적, H2 호환 | 마이그레이션에서 직접 불가 | **권장** |
| PostgreSQL `gen_random_uuid()` | DB에서 직접 생성 | pgcrypto 확장 필요 | - |
| PostgreSQL `uuid-ossp` | 표준 | 별도 확장 설치 | - |

#### H2 테스트 환경 호환

```sql
-- H2용 마이그레이션 (부분 인덱스 미지원)
-- src/test/resources/db/migration/V{version}__add_guest_token_to_quiz_sessions.sql
ALTER TABLE quiz_sessions ADD COLUMN guest_token VARCHAR(36);
CREATE UNIQUE INDEX idx_quiz_sessions_guest_token ON quiz_sessions(guest_token);
```

---

## 사이드 이펙트

### 1. API 응답 변경

> **참고**: 응답에 필드 추가는 일반적으로 **하위 호환** (Breaking Change 아님)
> 진짜 Breaking Change는 요청 파라미터 변경 시 발생

| 변경 유형 | 내용 | Breaking? |
|----------|------|:---------:|
| 응답 필드 추가 (`guestToken`) | 새 필드 추가 | **No** |
| 요청 파라미터 변경 (향후) | `sessionId` → `guestToken` | **Yes** |

```json
// Before
{ "sessionId": 1, "userId": null, "quizId": 1, ... }

// After (하위 호환)
{ "sessionId": 1, "guestToken": "uuid-...", "userId": null, "quizId": 1, ... }
```

**프론트엔드 대응:**
- `guestToken` 저장 로직 추가 (HttpOnly Cookie 권장)
- 추천 요청 시 `guestToken` 전달
- 병행 지원 기간: `sessionId`와 `guestToken` 모두 허용 (2주)

### 2. 기존 데이터 처리

| 상황 | 처리 방안 |
|------|----------|
| 기존 `user_id=null` 세션 | 마이그레이션으로 `guestToken` 부여 |
| 기존 `user_id` 있는 세션 | `guestToken=null` 유지 |

### 3. isAnonymous() 로직 변경

```java
// Before
public boolean isAnonymous() {
    return this.user == null;
}

// After - 선택지
// A: guestToken 기준 (권장)
public boolean isAnonymous() {
    return this.guestToken != null && this.user == null;
}

// B: 하위 호환성 유지
public boolean isAnonymous() {
    return this.user == null;  // 기존 로직 유지
}

public boolean hasGuestToken() {
    return this.guestToken != null;  // 새 메서드 추가
}
```

---

## 대안 비교

| 방식 | 영향 범위 | 프론트 변경 | DB 변경 | 보안성 |
|------|----------|------------|---------|--------|
| Guest Token (이 문서) | 12+ 파일 | 토큰 저장 로직 | 컬럼 추가 | 높음 |
| HTTP 세션 | 3-4 파일 | 없음 | 컬럼 추가 | 중간 |
| 현행 유지 (null) | 0 | 세션ID 저장 | 없음 | 낮음 |

---

## 작업 순서

### Phase 1: 기반 작업
- [ ] DB 마이그레이션 스크립트 작성
- [ ] `QuizSession` 엔티티에 `guestToken` 필드 추가
- [ ] `QuizSessionRepository`에 조회 메서드 추가

### Phase 2: 비즈니스 로직
- [ ] `QuizService.startQuiz()` - 토큰 생성 로직 추가
- [ ] `RecommendationService.generateRecommendation()` - 토큰 기반 연동

### Phase 3: API 응답
- [ ] `QuizSessionStartResponse`에 `guestToken` 필드 추가
- [ ] Swagger 문서 업데이트

### Phase 4: 테스트
- [ ] 단위 테스트 수정 (5개 이상)
- [ ] 통합 테스트: 비로그인 → 퀴즈 → 회원가입 → 추천 시나리오

### Phase 5: 프론트엔드 연동
- [ ] 프론트엔드에 `guestToken` 저장 로직 전달
- [ ] E2E 테스트

---

## 체크리스트

- [ ] DB 마이그레이션 스크립트 작성
- [ ] QuizSession 엔티티 수정
- [ ] QuizSessionRepository 메서드 추가
- [ ] QuizService 수정
- [ ] RecommendationService 수정
- [ ] QuizSessionStartResponse DTO 수정
- [ ] 테스트 코드 수정 (5개 이상)
- [ ] Swagger 문서 업데이트
- [ ] 프론트엔드 연동 가이드 작성
