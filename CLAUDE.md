# IPZY Backend

## WHY

패션 선택에 어려움을 겪는 사용자를 위한 **AI 기반 코디 추천 서비스**.
스타일 퀴즈를 통해 사용자 취향을 파악하고, 개인화된 코디를 추천합니다.

## 기술 스택

- Java 21, Spring Boot 3.2.0, PostgreSQL 16
- Spring Security + Session (OAuth2)
- SpringDoc OpenAPI (Swagger)

## 빠른 시작

```bash
cp .env.example .env              # 환경변수 설정
docker compose up postgres -d     # DB 실행
./gradlew bootRun                 # 앱 실행
```

## 패키지 구조

```
com.ipzy
├── domain/{도메인}/       # controller, service, repository, entity, dto, exception
└── _global/              # common, config, exception
```

**핵심 진입점:**
- `_global/config/SecurityConfig.java` - 인증/인가
- `_global/exception/GlobalExceptionHandler.java` - 예외 처리
- `_global/common/ApiResponse.java` - 응답 래퍼

## 주요 명령어

```bash
./gradlew bootRun         # 실행
./gradlew test            # 테스트
./gradlew clean build     # 빌드
```

## 저장소 관례

- 브랜치: `feature/*`, `hotfix/*` → `dev` → `main`
- 커밋: Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`)

## 참조 문서

필요할 때 아래 문서를 참고하세요:

| 문서 | 경로 |
|------|------|
| **코드 패턴** | `docs/reference/ipzy-code-patterns.md` |
| 코드 컨벤션 | `docs/reference/code-conventions.md` |
| 에러 코드 | `docs/reference/error-codes.md` |
| ERD | `docs/reference/erd.md` |
| Swagger 가이드 | `docs/reference/swagger-guide.md` |

Swagger UI: http://localhost:8080/swagger-ui.html
