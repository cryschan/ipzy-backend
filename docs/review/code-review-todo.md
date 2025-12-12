# 코드 리뷰 수정 TODO

> 생성일: 2025-12-12
> 기반: [code-review-report.md](./code-review-report.md)

---

## 우선순위 정의

| 등급 | 설명 | 목표 기한 |
|------|------|----------|
| **P0** | 보안 취약점, 즉시 수정 | 1일 이내 |
| **P1** | 중요 이슈, 단기 수정 | 1주 이내 |
| **P2** | 개선 사항, 중기 수정 | 2주 이내 |
| **P3** | 권장 사항, 장기 개선 | 1개월 이내 |

---

## _global (공통)

### SecurityConfig.java

- [ ] **P0** Admin API 인증 추가
  - 파일: `_global/config/SecurityConfig.java:50`
  - 현재: `.requestMatchers("/api/admin/**").permitAll()`
  - 수정: `.requestMatchers("/api/admin/**").hasRole("ADMIN")`

- [ ] **P1** Test API 처리
  - 파일: `_global/config/SecurityConfig.java:51`
  - 옵션 A: 프로파일 조건부 활성화 (`@Profile("!prod")`)
  - 옵션 B: 엔드포인트 제거

---

## Auth 도메인

### 테스트

- [ ] **P2** OAuth2LogoutSuccessHandler 테스트 추가
  - 위치: `src/test/java/com/ipzy/domain/auth/handler/`
  - 케이스: 로그아웃 성공, 토큰 revoke 실패 처리

### 문서화

- [ ] **P3** OAuth2 흐름 시퀀스 다이어그램 추가
  - 위치: `docs/auth/oauth2-flow.md`

---

## User 도메인

### 테스트

- [ ] **P1** WithdrawalService 테스트 작성
  - 위치: `src/test/java/com/ipzy/domain/user/service/WithdrawalServiceTest.java`
  - 필수 케이스:
    - [ ] 탈퇴 성공 (OAuth 연결 해제 포함)
    - [ ] 존재하지 않는 사용자 탈퇴 시도
    - [ ] OAuth 연결 해제 실패 시 처리

- [ ] **P2** UserService 테스트 보강
  - 추가 케이스:
    - [ ] 프로필 업데이트 부분 필드만 변경
    - [ ] 스타일 선호도 업데이트

---

## Recommendation 도메인

### 예외 처리

- [ ] **P2** ErrorCode 네이밍 통일
  - 파일: `domain/recommendation/exception/RecommendationErrorCode.java`
  - 현재: `REC001`, `REC002`
  - 수정: `REC_001`, `REC_002`

### 테스트

- [ ] **P1** RecommendationService 테스트 보강
  - 위치: `src/test/java/com/ipzy/domain/recommendation/service/`
  - 추가 케이스:
    - [ ] AI 서버 타임아웃 처리
    - [ ] AI 서버 응답 유효성 검증 실패
    - [ ] 빈 추천 결과 처리

- [ ] **P2** PythonAiClient 단위 테스트 추가
  - MockRestServiceServer 활용

---

## Quiz 도메인

### 예외 처리

- [ ] **P2** QuizException static 팩토리 패턴 적용
  - 파일: `domain/quiz/exception/QuizException.java`
  - 수정 내용:
    ```java
    // Before
    public QuizException(QuizErrorCode errorCode) { ... }

    // After
    private QuizException(QuizErrorCode errorCode) { ... }
    public static QuizException sessionNotFound(Long sessionId) { ... }
    public static QuizException invalidAnswer(Long questionId) { ... }
    ```

### 코드 품질

- [ ] **P2** QuizService 책임 분리
  - 현재: 362줄, 다중 책임
  - 분리 방안:
    - [ ] `QuizSessionService` - 세션 생성/조회
    - [ ] `QuizAnswerService` - 답변 저장/검증
    - [ ] `QuizResultCalculator` - 결과 계산 (선택)

- [ ] **P3** QuizService 로깅 추가
  - `@Slf4j` 어노테이션 추가
  - 주요 흐름에 로그 추가

- [ ] **P3** validateAnswers() 메서드 분리
  - 현재: ~70줄
  - 분리:
    - [ ] `validateQuestionExists()`
    - [ ] `validateOptionExists()`
    - [ ] `saveAnswers()`

### 테스트

- [ ] **P1** QuizService 테스트 작성
  - 위치: `src/test/java/com/ipzy/domain/quiz/service/QuizServiceTest.java`
  - 필수 케이스:
    - [ ] 세션 생성 성공
    - [ ] 답변 제출 성공
    - [ ] 존재하지 않는 세션 조회
    - [ ] 잘못된 답변 제출

---

## 진행 체크리스트

### P0 (즉시)
- [ ] SecurityConfig Admin API 수정

### P1 (1주)
- [ ] SecurityConfig Test API 처리
- [ ] WithdrawalServiceTest 작성
- [ ] RecommendationServiceTest 보강
- [ ] QuizServiceTest 작성

### P2 (2주)
- [ ] RecommendationErrorCode 네이밍 수정
- [ ] QuizException static 팩토리 적용
- [ ] QuizService 책임 분리
- [ ] OAuth2LogoutSuccessHandler 테스트
- [ ] UserServiceTest 보강
- [ ] PythonAiClient 테스트

### P3 (1개월)
- [ ] QuizService 로깅 추가
- [ ] validateAnswers() 메서드 분리
- [ ] OAuth2 흐름 문서화

---

## 완료 기록

| 날짜 | 항목 | 담당자 |
|------|------|--------|
| - | - | - |

---

## 관련 문서

- [리뷰 리포트](./code-review-report.md)
- [리뷰 계획](./code-review-plan.md)
