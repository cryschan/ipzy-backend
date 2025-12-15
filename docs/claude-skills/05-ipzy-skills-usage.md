# IPZY Skills 사용 가이드

이 프로젝트에 설치된 6개의 Skills 상세 설명 및 사용법입니다.

## 설치된 Skills 목록

| # | 스킬명 | 용도 |
|---|--------|------|
| 1 | ipzy-domain-generator | 새 도메인 패키지 전체 생성 |
| 2 | ipzy-api-endpoint | 기존 도메인에 API 추가 |
| 3 | ipzy-test-generator | Service/Controller 테스트 생성 |
| 4 | ipzy-code-reviewer | 코드 품질 검토 |
| 5 | ipzy-exception-handler | ErrorCode + Exception 추가 |
| 6 | ipzy-dto-generator | Request/Response DTO 생성 |

---

## 1. ipzy-domain-generator

### 용도
새로운 도메인 패키지 전체 구조를 한 번에 생성합니다.

### 생성되는 파일
```
domain/{도메인}/
├── controller/{Domain}Controller.java
├── service/{Domain}Service.java
├── repository/{Domain}Repository.java
├── entity/{Domain}.java
├── dto/
│   ├── {Domain}Request.java
│   └── {Domain}Response.java
└── exception/
    ├── {Domain}ErrorCode.java
    └── {Domain}Exception.java
```

### 트리거 예시
```
"coupon 도메인 만들어줘"
"notification 도메인 생성해줘"
"새로운 review 모듈 추가해줘"
```

### 사용 시나리오
- 새로운 기능 모듈 개발 시작
- MVP 빠르게 구축
- 팀원에게 표준 구조 전달

---

## 2. ipzy-api-endpoint

### 용도
기존 도메인에 새로운 API 엔드포인트를 추가합니다.

### 생성되는 코드
- Controller 메서드 (Swagger 문서화 포함)
- Service 메서드
- DTO (필요시)

### 트리거 예시
```
"user 도메인에 프로필 이미지 업로드 API 추가해줘"
"product에 검색 API 만들어줘"
"quiz에 결과 조회 엔드포인트 추가해줘"
```

### 지원 HTTP 메서드
| 메서드 | 패턴 |
|--------|------|
| GET (단건) | `ApiResponse<T>` |
| GET (목록) | `ApiResponse<List<T>>` |
| GET (페이징) | `ApiResponse<PageResponse<T>>` |
| POST | `ApiResponse<T>` |
| PUT | `ApiResponse<T>` |
| DELETE | `ResponseEntity<Void>` |

---

## 3. ipzy-test-generator

### 용도
Service 또는 Controller의 테스트 코드를 생성합니다.

### 테스트 구조
```java
@Nested
@DisplayName("메서드명")
class MethodName {
    @Test
    @DisplayName("성공 - 설명")
    void success() { }

    @Test
    @DisplayName("실패 - 사유")
    void fail_reason() { }
}
```

### 트리거 예시
```
"UserService 테스트 코드 작성해줘"
"QuizController 유닛 테스트 만들어줘"
"recommendation 도메인 테스트 생성해줘"
```

### 테스트 패턴
- given-when-then 구조
- BDDMockito 사용 (`given`, `then`)
- AssertJ 사용 (`assertThat`, `assertThatThrownBy`)

---

## 4. ipzy-code-reviewer

### 용도
코드가 IPZY 프로젝트 규칙을 준수하는지 검토합니다.

### 검토 항목
| 카테고리 | 체크 내용 |
|----------|----------|
| Controller | ApiResponse 래퍼, Swagger 문서화 |
| Service | @Transactional, Response DTO 반환 |
| DTO | Java record, @Schema |
| Exception | 코드 네이밍, static factory |
| Entity | BaseEntity 상속, soft delete |
| 보안 | SQL Injection, 민감정보 로깅 |
| 성능 | N+1 쿼리, FetchType |

### 트리거 예시
```
"이 PR 코드 리뷰해줘"
"UserService 코드 검토해줘"
"최근 변경사항 리뷰해줘"
```

### 출력 형식
```markdown
## 코드 리뷰 결과

### 준수 사항
- ...

### 개선 권장
- 파일:라인 - 설명

### 필수 수정
- 파일:라인 - 설명
```

---

## 5. ipzy-exception-handler

### 용도
새로운 ErrorCode와 Exception static factory를 추가합니다.

### ErrorCode 네이밍 규칙
```
{DOMAIN}_{3자리숫자}

예: USER_001, QUIZ_003, REC_101
```

### 코드 범위
| 범위 | 용도 |
|------|------|
| 001-099 | 클라이언트 에러 (4xx) |
| 101-199 | 서버/외부 연동 에러 (5xx) |
| 201-299 | 비즈니스 로직 에러 |
| 301-399 | 권한 관련 에러 |

### 트리거 예시
```
"User 도메인에 SUSPENDED 에러코드 추가해줘"
"Quiz에 타임아웃 에러 처리 추가해줘"
"recommendation에 AI 서비스 에러 추가해줘"
```

### 생성 패턴
```java
// ErrorCode
NEW_ERROR(HttpStatus.BAD_REQUEST, "DOMAIN_004", "에러 메시지");

// Exception
public static DomainException newError() {
    return new DomainException(DomainErrorCode.NEW_ERROR);
}
```

---

## 6. ipzy-dto-generator

### 용도
Request/Response/Command DTO를 생성합니다.

### DTO 종류
| 종류 | 네이밍 | 용도 |
|------|--------|------|
| Request | `Create{Domain}Request` | API 입력 |
| Response | `{Domain}Response` | API 출력 |
| Command | `Update{Domain}Command` | Service 입력 |

### 트리거 예시
```
"User 프로필 수정 Request DTO 만들어줘"
"Product 상세 Response 생성해줘"
"Quiz 결과 응답 DTO 추가해줘"
```

### 주요 패턴
```java
// Request: toEntity() 메서드
public {Domain} toEntity() { }

// Response: from() 정적 팩토리
public static {Domain}Response from({Domain} entity) { }

// Command: from() 변환
public static Command from(Request request) { }
```

---

## Skills 조합 사용 예시

### 새 기능 개발 워크플로우

```
1. "notification 도메인 만들어줘"
   → ipzy-domain-generator 실행

2. "알림 읽음 처리 API 추가해줘"
   → ipzy-api-endpoint 실행

3. "NotificationService 테스트 만들어줘"
   → ipzy-test-generator 실행

4. "코드 리뷰해줘"
   → ipzy-code-reviewer 실행
```

### 에러 처리 추가 워크플로우

```
1. "Notification에 이미 읽은 알림 에러 추가해줘"
   → ipzy-exception-handler 실행

2. "docs/reference/error-codes.md 업데이트해줘"
   → 문서 자동 업데이트
```

---

## 참고

- 스킬 파일 위치: `.claude/skills/`
- 프로젝트 규칙: `docs/reference/ipzy-code-patterns.md`
- 에러 코드 목록: `docs/reference/error-codes.md`
