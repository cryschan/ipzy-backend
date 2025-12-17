---
name: ipzy-cross-review
description: Claude + Codex 교차 검증 코드 리뷰. PR 전 품질 검증, 보안 체크, IPZY 패턴 준수 확인 시 사용.
allowed-tools: Read, Grep, Glob, Bash, Write, AskUserQuestion
---

# IPZY Cross Review

Claude와 Codex의 교차 검증을 통해 코드 품질을 보장합니다.

## 핵심 원칙

- **Claude**: 1차 리뷰, IPZY 패턴 검증, 수정 구현
- **Codex**: 교차 검증, 버그 탐지, 보안 취약점 분석
- **교차 검증**: 두 AI의 관점을 종합하여 품질 향상

## 워크플로우

```
코드 변경 완료
       ↓
[Phase 1] Claude 1차 리뷰
       ↓
[Phase 2] Codex 교차 검증
       ↓
[Phase 3] 결과 비교/종합
       ↓
[Phase 4] 이슈 수정 (필요시)
       ↓
[Phase 5] 재검증 (필요시)
       ↓
[Phase 6] PR 체크리스트 검증
       ↓
최종 리뷰 리포트 출력 + PR 준비 완료
```

## Phase 1: Claude 1차 리뷰

### 변경 파일 수집

```bash
git diff HEAD --name-only
git diff HEAD --stat
```

### Claude 체크 항목

#### IPZY 패턴 준수
- [ ] DTO 패턴: Request/Response 분리
- [ ] Exception 패턴: `{Domain}Exception.{상황}()` static factory
- [ ] Service 패턴: `@Transactional(readOnly = true/false)`
- [ ] Controller 패턴: `@AuthenticationPrincipal` + `validatePrincipal()`
- [ ] Swagger 문서화: `@Tag`, `@Operation`, `@Schema`

#### 코드 품질
- [ ] 메서드 길이 적절 (20줄 이하 권장)
- [ ] 클래스 책임 단일화
- [ ] 네이밍 명확성
- [ ] 중복 코드 없음

#### 테스트
- [ ] 테스트 코드 존재
- [ ] 성공/실패 케이스 커버
- [ ] BDDMockito 패턴 사용

## Phase 2: Codex 교차 검증

### 사용자 설정 확인

`AskUserQuestion`으로 확인:
- Codex 모델: `gpt-5.2` (기본) 또는 `o4-mini`
- Reasoning effort: `low`, `medium`, `high`

### Codex 검증 실행

```bash
# 변경된 파일 리뷰 요청
echo "Review this Spring Boot code for:
1. Security vulnerabilities (SQL injection, XSS, auth bypass)
2. Performance issues (N+1 queries, missing indexes)
3. Logic errors and edge cases
4. Best practices violations

Changed files:
$(git diff HEAD)

Project context:
- Spring Boot 3.x + JPA + PostgreSQL
- OAuth2 authentication (Kakao)
- Domain-driven package structure" | codex exec -m <model> --config model_reasoning_effort="<effort>" --sandbox read-only
```

### Codex 체크 항목

#### 보안
- [ ] SQL Injection 취약점
- [ ] XSS 취약점
- [ ] 인증/인가 우회 가능성
- [ ] 민감 정보 노출

#### 성능
- [ ] N+1 쿼리 문제
- [ ] 불필요한 DB 호출
- [ ] 메모리 누수 가능성

#### 로직
- [ ] 엣지 케이스 처리
- [ ] null 처리
- [ ] 동시성 이슈

## Phase 3: 결과 비교/종합

### 종합 리포트 생성

```markdown
# Cross Review 결과

> 리뷰일: {YYYY-MM-DD HH:MM}
> 대상: {변경 파일 목록}
> 리뷰어: Claude + Codex

---

## 요약

| 항목 | Claude | Codex | 종합 |
|------|--------|-------|------|
| 보안 | {결과} | {결과} | {최종} |
| 성능 | {결과} | {결과} | {최종} |
| 품질 | {결과} | {결과} | {최종} |
| IPZY 패턴 | {결과} | - | {최종} |

## 발견된 이슈

### Critical (즉시 수정 필요)

| # | 이슈 | 발견자 | 파일 | 라인 |
|---|------|--------|------|------|
| 1 | {이슈} | Claude/Codex | {파일} | {라인} |

### Warning (권장 수정)

| # | 이슈 | 발견자 | 파일 | 라인 |
|---|------|--------|------|------|
| 1 | {이슈} | Claude/Codex | {파일} | {라인} |

### Info (참고)

| # | 이슈 | 발견자 | 파일 | 라인 |
|---|------|--------|------|------|
| 1 | {이슈} | Claude/Codex | {파일} | {라인} |

---

## Claude 상세 리뷰

### IPZY 패턴 준수

{상세 내용}

### 코드 품질

{상세 내용}

---

## Codex 상세 리뷰

### 보안 분석

{Codex 원문}

### 성능 분석

{Codex 원문}

---

## 수정 권장 사항

### 필수 수정

1. {수정 사항}
   - 파일: `{파일}`
   - 현재: `{현재 코드}`
   - 권장: `{권장 코드}`

### 선택 수정

1. {수정 사항}

---

## 결론

- [ ] **통과**: 이슈 없음, PR 진행 가능
- [ ] **조건부 통과**: Warning 수정 권장
- [ ] **재검토 필요**: Critical 이슈 수정 후 재리뷰
```

## Phase 4: 이슈 수정

Critical 이슈 발견 시:

1. 사용자에게 수정 여부 확인 (`AskUserQuestion`)
2. Claude가 수정 구현
3. 변경 사항 요약

## Phase 5: 재검증

수정 후 재검증:

```bash
# 세션 유지하며 재검증
echo "Review the updated implementation:
$(git diff HEAD)

Previous issues fixed:
- {수정된 이슈 목록}

Verify fixes and check for new issues." | codex exec resume --last
```

재검증 통과까지 반복.

## Phase 6: PR 체크리스트 검증

리뷰 완료 후 PR 생성 전 최종 검증:

### 자동 검증 항목

```bash
# 1. 브랜치명 규칙 확인
git branch --show-current | grep -E "^(feature|fix|hotfix|refactor|docs)/"

# 2. dev 브랜치 최신화 확인
git fetch origin dev
git log HEAD..origin/dev --oneline | wc -l  # 0이면 최신

# 3. 코드 포맷팅 (spotless 있는 경우)
./gradlew spotlessCheck 2>/dev/null || echo "spotless 미설정"

# 4. 안쓰는 import 확인
grep -r "^import " src/main/java --include="*.java" | \
  grep -v "import static" | \
  sort | uniq -c | sort -rn | head -20
```

### 수동 확인 항목

- [ ] PR 대상 브랜치가 `dev`인가?
- [ ] 커밋 메시지가 명확한가?
- [ ] 테스트 코드가 통과하는가?
- [ ] Swagger 문서가 업데이트 되었는가? (API 변경시)

### 체크리스트 출력 형식

```markdown
## PR 체크리스트

| 항목 | 상태 | 비고 |
|------|------|------|
| 브랜치명 규칙 | ✅/❌ | `{브랜치명}` |
| PR 대상: dev | ✅/❌ | |
| dev 최신화 | ✅/❌ | {n}커밋 뒤처짐 |
| 코드 포맷팅 | ✅/⚠️ | spotless/수동확인 |
| 미사용 import | ✅/⚠️ | {n}개 발견 |
| 테스트 통과 | ✅/❌ | |
```

## 사용 방법

### 기본 사용

```
"코드 리뷰해줘"
"cross review 해줘"
"PR 전에 검토해줘"
```

### 특정 파일 지정

```
"RecommendationService 리뷰해줘"
"user 도메인 변경사항 검토해줘"
```

### 특정 관점 강조

```
"보안 중심으로 리뷰해줘"
"성능 위주로 검토해줘"
```

## 출력 예시

### 입력
```
"방금 작업한 추천 API 리뷰해줘"
```

### 출력
```markdown
# Cross Review 결과

> 리뷰일: 2025-12-15 15:30
> 대상: RecommendationController.java, RecommendationService.java
> 리뷰어: Claude + Codex

---

## 요약

| 항목 | Claude | Codex | 종합 |
|------|--------|-------|------|
| 보안 | Pass | Pass | ✅ |
| 성능 | Warning | Pass | ⚠️ |
| 품질 | Pass | Pass | ✅ |
| IPZY 패턴 | Pass | - | ✅ |

## 발견된 이슈

### Warning (권장 수정)

| # | 이슈 | 발견자 | 파일 | 라인 |
|---|------|--------|------|------|
| 1 | N+1 쿼리 가능성 | Claude | RecommendationService.java | 45 |

---

## 수정 권장 사항

### 선택 수정

1. N+1 쿼리 개선
   - 파일: `RecommendationService.java:45`
   - 현재: `recommendations.forEach(r -> r.getItems())`
   - 권장: `@EntityGraph` 또는 `JOIN FETCH` 사용

---

## 결론

- [x] **조건부 통과**: Warning 수정 권장, PR 진행 가능
```

## Codex 명령어 레퍼런스

| 상황 | 명령어 |
|------|--------|
| 계획 검증 | `echo "plan" \| codex exec --sandbox read-only` |
| 코드 리뷰 | `echo "review" \| codex exec --sandbox read-only` |
| 세션 유지 | `echo "continue" \| codex exec resume --last` |
| 모델 지정 | `codex exec -m gpt-4.1` |
| 추론 수준 | `--config model_reasoning_effort="high"` |

## 트리거 키워드

- "리뷰해줘", "검토해줘", "review"
- "cross review", "교차 검증"
- "PR 전에", "머지 전에"
- "품질 체크", "코드 체크"
- "PR 체크리스트", "PR 준비"
- "브랜치 확인", "커밋 전 확인"
