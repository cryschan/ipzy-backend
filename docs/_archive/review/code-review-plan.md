# tato126 코드베이스 리뷰 계획

> 기반 문서: `~/.claude/learnings/` 베스트 프랙티스
> 상태: ✅ **완료** (2025-12-12)

## 산출물

| 문서 | 설명 |
|------|------|
| [code-review-report.md](./code-review-report.md) | 도메인별 리뷰 결과 |
| [code-review-todo.md](./code-review-todo.md) | 우선순위별 수정 체크리스트 |

---

## 리뷰 범위

tato126 작성 주요 도메인:
- **Auth**: OAuth2 카카오 로그인, 로그아웃, 세션 관리
- **User**: 프로필 조회/수정, 탈퇴
- **Recommendation**: Python AI 통신, 추천 API

---

## Phase 1: Security 리뷰 ✅

> 참조: `Security/java_spring_security_2025.md`

### 1.1 SecurityConfig 점검
- [x] CSRF 설정 적절성 (SPA + Session 환경)
- [x] CORS 설정 검증
- [x] 인증 엔드포인트 보호 확인 → **P0 이슈 발견**
- [x] 세션 관리 정책 (동시 로그인, 세션 고정 공격 방어)

### 1.2 OAuth2 인증 흐름
- [x] OAuth2 토큰 저장/관리 방식
- [x] Provider 토큰 revoke 로직 검증
- [x] 로그아웃 시 세션/토큰 정리 완전성

### 1.3 입력 검증
- [x] Bean Validation 적용 여부
- [x] SQL Injection 방어 (JPA parameterization)
- [x] XSS 방어 (출력 인코딩)

**대상 파일:**
- `_global/config/SecurityConfig.java`
- `domain/auth/handler/*.java`
- `domain/auth/service/*.java`

---

## Phase 2: Exception Handling 리뷰 ✅

> 참조: `Architecture/exception_handling_patterns_2025.md`

### 2.1 예외 계층 구조
- [x] BaseException → DomainException 계층 준수 여부
- [x] ErrorCode Enum 중앙 관리 여부
- [x] HTTP 상태 코드 매핑 적절성

### 2.2 GlobalExceptionHandler
- [x] 모든 예외 타입 처리 여부
- [x] 일관된 ErrorResponse 형식
- [x] 로깅 적절성 (민감정보 노출 금지)

### 2.3 도메인별 예외
- [x] static 팩토리 메서드 패턴 준수 → **P2 이슈 (QuizException)**
- [x] 예외 메시지 명확성
- [x] 예외 발생 위치 적절성 (Service vs Repository)

**대상 파일:**
- `_global/exception/GlobalExceptionHandler.java`
- `domain/*/exception/*.java`

---

## Phase 3: Spring Boot 3 패턴 리뷰 ✅

> 참조: `Development/spring_boot3_production_2025.md`

### 3.1 Jakarta EE 전환
- [x] `jakarta.*` 패키지 사용 확인
- [x] 더 이상 `javax.*` 미사용 확인

### 3.2 Controller 패턴
- [x] `@RestController` + `@RequestMapping` 조합
- [x] ResponseEntity vs ApiResponse 일관성
- [x] 적절한 HTTP 메서드 사용

### 3.3 Service 패턴
- [x] `@Transactional` 적용 범위
- [x] 읽기 전용 트랜잭션 (`readOnly=true`)
- [x] 서비스 간 의존성 방향

### 3.4 Repository 패턴
- [x] Spring Data JPA 메서드 네이밍 규칙
- [x] N+1 문제 방지 (fetch join, @EntityGraph)
- [x] Optional 반환 처리

**대상 파일:**
- `domain/*/controller/*.java`
- `domain/*/service/*.java`
- `domain/*/repository/*.java`

---

## Phase 4: 테스트 리뷰 ✅

> 참조: `Development/testing_strategies_2025.md`

### 4.1 테스트 구조
- [x] `@Nested` + `@DisplayName` 적용
- [x] Given-When-Then 패턴
- [x] 성공/실패 케이스 분리

### 4.2 테스트 커버리지
- [x] Happy path 테스트 존재
- [x] Edge case 테스트 존재 → **P1 이슈 (25% 커버리지)**
- [x] 예외 케이스 테스트 존재

### 4.3 Mock 사용
- [x] BDDMockito 스타일 준수
- [x] 과도한 Mock 사용 여부
- [x] 통합 테스트 존재 여부

**대상 파일:**
- `src/test/java/com/ipzy/domain/*/service/*Test.java`
- `src/test/java/com/ipzy/domain/*/controller/*Test.java`

---

## Phase 5: 코드 품질 리뷰 ✅

> 참조: `Design/code_review_checklist_java_2025.md`

### 5.1 SOLID 원칙
- [x] SRP: 클래스/메서드 단일 책임 → **P2 이슈 (QuizService)**
- [x] OCP: 확장에 열림, 수정에 닫힘
- [x] DIP: 인터페이스 의존

### 5.2 클린 코드
- [x] 메서드 길이 적절성 (< 20줄 권장) → **P3 이슈 (validateAnswers)**
- [x] 중첩 깊이 (< 3단계)
- [x] 네이밍 명확성
- [x] 주석 필요성 (코드로 설명 가능한지)

### 5.3 DTO 패턴
- [x] Record 사용 여부
- [x] `from()` 팩토리 메서드
- [x] Bean Validation 어노테이션

---

## 리뷰 우선순위

| 순서 | Phase | 이유 |
|------|-------|------|
| 1 | Security | 보안 취약점은 치명적 |
| 2 | Exception | 에러 처리 일관성 영향 큼 |
| 3 | Spring Boot 3 | 아키텍처 패턴 기반 |
| 4 | 코드 품질 | 유지보수성 |
| 5 | 테스트 | 신뢰도 검증 |

---

## 예상 산출물

1. **리뷰 리포트**: 각 Phase별 발견사항
2. **개선 제안**: 우선순위별 TODO 목록
3. **코드 수정**: 승인된 개선사항 적용

---

## 진행 방식

```
Phase 1 → 보고 → 승인 → Phase 2 → 보고 → 승인 → ...
```

각 Phase 완료 후 결과 보고 및 승인 요청
