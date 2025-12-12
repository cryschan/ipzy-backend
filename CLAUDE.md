# IPZY Backend

패션 스타일 퀴즈 기반 코디 추천 서비스 백엔드

## 기술 스택

- Java 21, Spring Boot 3.2.0, PostgreSQL 16
- Spring Security + Session (OAuth2)
- SpringDoc OpenAPI (Swagger)

## 개발 환경 설정

```bash
# 필수: Java 21, Docker
cp .env.example .env              # 환경변수 설정
docker compose up postgres -d     # DB 실행
./gradlew bootRun                 # 앱 실행
```

**환경변수** (`.env`): DB 접속정보, Kakao OAuth, AI_SERVICE_URL

## 패키지 구조

```
com.ipzy
├── domain/{도메인}/       # controller, service, repository, entity, dto, exception
└── _global/              # common(ApiResponse, BaseEntity), config, exception
```

**핵심 파일:**
- `_global/config/SecurityConfig.java` - 인증/인가 설정
- `_global/exception/GlobalExceptionHandler.java` - 전역 예외 처리
- `_global/common/ApiResponse.java` - 응답 래퍼

## 네이밍 규칙

| 계층 | 패턴 | 예시 |
|------|------|------|
| Controller | `{Domain}Controller` | `UserController` |
| Service | `{Domain}Service` | `UserService` |
| Repository | `{Entity}Repository` | `UserRepository` |
| Request DTO | `{Action}{Entity}Request` | `UpdateProfileRequest` |
| Response DTO | `{Entity}Response` | `UserProfileResponse` |
| Exception | `{Domain}Exception` | `UserException` |

## 필수 어노테이션

- **Controller**: `@RestController`, `@RequiredArgsConstructor`, `@Tag`
- **Service**: `@Service`, `@Transactional(readOnly=true)`, `@Slf4j`
- **Entity**: `@Entity`, `@Getter`, `@NoArgsConstructor(access=PROTECTED)`, `@Builder`

## 핵심 패턴

- **DTO**: Record + Bean Validation, `from()` 팩토리 → `domain/auth/dto/AuthMeResponse.java`
- **예외**: 도메인별 Exception + static 팩토리 → `domain/user/exception/UserException.java`

## 저장소 관례

**브랜치 전략:**
- `feature/*`, `hotfix/*` → `dev` → `main`
- PR 필수, dev가 기본 브랜치

**커밋 메시지:** Conventional Commits
```
feat: 새 기능 추가
fix: 버그 수정
docs: 문서 변경
refactor: 리팩토링
test: 테스트 추가/수정
```

## 테스트

```bash
./gradlew test                              # 전체 테스트
./gradlew test --tests "*UserServiceTest"   # 단일 클래스
./gradlew test --tests "*UserServiceTest.success*"  # 단일 메서드
```

**테스트 패턴**: `@Nested` + `@DisplayName`, Given-When-Then, BDDMockito
- 메서드명: `success()`, `fail_{실패이유}()`

## 주요 명령어

```bash
./gradlew bootRun         # 실행
./gradlew test            # 테스트
./gradlew clean build     # 빌드
docker compose up         # 전체 실행 (DB + App)
```

## API 응답 형식

```json
{ "success": true, "data": {...}, "error": null }
{ "success": false, "data": null, "error": { "code": "USER_001", "message": "..." } }
```

## ErrorCode 네이밍

- 형식: `{DOMAIN}_{번호}` (예: `USER_001`, `AUTH_001`, `REC_001`)

## 문서 구조

```
docs/
├── domain/        # 도메인별 계획/구현 문서 (user, recommendation, product)
├── reference/     # 참조 문서 (ERD, 에러코드, 코드패턴)
├── review/        # 코드 리뷰
└── todo/          # 단기 TODO
```

## 참조 문서

- 에러 코드: `docs/reference/error-codes.md`
- 도메인 ERD: `docs/reference/erd.md`
- 코드 패턴: `docs/reference/raeperd-crud-patterns.md`
- 설계 참고: `docs/reference/raeperd-realworld-analysis.md`
- 코드 리뷰: `docs/review/code-review-report.md`
- Swagger: http://localhost:8080/swagger-ui.html

## 주의사항

- Swagger `ApiResponse` 이름 충돌 → FQCN 사용: `@io.swagger.v3.oas.annotations.responses.ApiResponse`
- 테스트 DB는 H2 인메모리 사용 (application-test.yml)
