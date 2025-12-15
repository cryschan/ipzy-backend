---
name: ipzy-prompt-enhancer
description: 간단한 요청을 IPZY 프로젝트 컨텍스트 기반 상세 요구사항으로 변환. 복잡한 기능 구현 전 요구사항 명확화 시 사용.
allowed-tools: Read, Grep, Glob
---

# IPZY Prompt Enhancer

간단한 개발 요청을 IPZY 프로젝트 구조와 패턴을 분석하여 상세한 요구사항으로 변환합니다.

## 워크플로우

```
사용자 요청 (간단)
       ↓
[1단계] 프로젝트 컨텍스트 분석
       ↓
[2단계] 요청 의도 파악
       ↓
[3단계] 상세 요구사항 생성
       ↓
[4단계] 사용자 확인 요청
       ↓
확인 후 구현 진행
```

## 1단계: 프로젝트 컨텍스트 분석

### 분석 대상
- **기술 스택**: Spring Boot 3.x, JPA, PostgreSQL, OAuth2
- **아키텍처**: 도메인 기반 패키지 구조
- **기존 패턴**: 관련 도메인의 기존 구현 확인

### 분석 항목
```
src/main/java/com/ipzy/domain/{관련도메인}/
├── controller/  → API 패턴, Swagger 스타일
├── service/     → 트랜잭션 패턴, 반환 타입
├── repository/  → 쿼리 메서드 패턴
├── entity/      → 필드 구조, 연관관계
├── dto/         → Request/Response 패턴
└── exception/   → ErrorCode 형식
```

## 2단계: 요청 의도 파악

### 분류 기준
| 유형 | 키워드 | 예시 |
|------|--------|------|
| 새 기능 | 추가, 만들어, 구현 | "결제 기능 추가해줘" |
| 수정 | 변경, 수정, 업데이트 | "프로필 수정 API 바꿔줘" |
| 버그 수정 | 고쳐, 수정, 안됨 | "로그인이 안돼" |
| 리팩토링 | 정리, 개선, 분리 | "UserService 분리해줘" |
| 문서화 | 문서, Swagger, 설명 | "API 문서화해줘" |

## 3단계: 상세 요구사항 생성

### 출력 템플릿

```markdown
## 프로젝트 컨텍스트

### 기술 스택
- Spring Boot 3.x + JPA + PostgreSQL
- OAuth2 (카카오 로그인)
- 현재 도메인: {관련 도메인 목록}

### 기존 패턴 분석
- Controller: {기존 패턴 요약}
- Service: {기존 패턴 요약}
- Exception: {기존 패턴 요약}

---

## 요청 분석

### 요청 유형
{새 기능 / 수정 / 버그 수정 / 리팩토링}

### 핵심 요구사항
{사용자 요청의 핵심 정리}

---

## 구현 범위

### Controller Layer
- 엔드포인트: `{HTTP_METHOD} /api/{path}`
- 인증: {필요/불필요}
- 요청 DTO: `{RequestName}`
- 응답 DTO: `{ResponseName}`

### Service Layer
- 클래스: `{ServiceName}`
- 메서드: `{methodName}()`
- 트랜잭션: {readOnly / 쓰기}

### Repository Layer
- 필요한 쿼리 메서드: {메서드 목록}

### DTO
- Request: {필드 목록}
- Response: {필드 목록}

### Exception
- ErrorCode: `{DOMAIN}_{번호}` - {설명}

---

## 연관 영향

### 영향 받는 도메인
- {도메인1}: {영향 내용}
- {도메인2}: {영향 내용}

### 테스트 필요 범위
- {테스트 대상 목록}

---

## 성공 기준

- [ ] {기준 1}
- [ ] {기준 2}
- [ ] {기준 3}
- [ ] 테스트 코드 작성
- [ ] Swagger 문서화

---

## 추가 확인 필요 사항

- {불명확한 점 1}?
- {불명확한 점 2}?
```

## 4단계: 사용자 확인

### 확인 문구
```
위 요구사항으로 진행할까요?

수정이 필요하면 말씀해주세요:
- 범위 조정
- 추가 요구사항
- 우선순위 변경
```

## 중요 규칙

1. **구현 전 확인 필수**: 요구사항 확인 없이 절대 구현하지 않음
2. **기존 패턴 존중**: IPZY 프로젝트의 기존 코드 스타일 따름
3. **영향 범위 명시**: 변경으로 인한 영향 반드시 분석
4. **불명확한 점 질문**: 추측하지 말고 질문

## 사용 예시

### 입력
```
"회원 탈퇴 기능 추가해줘"
```

### 출력
```markdown
## 프로젝트 컨텍스트

### 기술 스택
- Spring Boot 3.x + JPA + PostgreSQL
- OAuth2 (카카오 로그인)

### 기존 패턴 분석
- Controller: @AuthenticationPrincipal + validatePrincipal() 패턴
- Service: soft delete 패턴 (status 변경)
- Exception: {Domain}Exception.{상황}() static factory

---

## 요청 분석

### 요청 유형
새 기능

### 핵심 요구사항
사용자가 계정을 탈퇴할 수 있는 기능

---

## 구현 범위

### Controller Layer
- 엔드포인트: `DELETE /api/users/me`
- 인증: 필요
- 응답: `204 No Content`

### Service Layer
- 클래스: `WithdrawalService` (신규)
- 메서드: `withdraw(Long userId)`
- 트랜잭션: 쓰기

### 연관 처리
- 카카오 OAuth 연결 해제 (unlink)
- User status → WITHDRAWN 변경
- 개인정보 마스킹 처리

### Exception
- `USER_010` - 이미 탈퇴한 사용자
- `USER_011` - 카카오 연결 해제 실패

---

## 연관 영향

### 영향 받는 도메인
- auth: 로그아웃 처리 필요
- recommendation: 사용자 추천 데이터 처리
- savedoutfit: 저장된 코디 처리

---

## 성공 기준

- [ ] 탈퇴 후 재로그인 불가
- [ ] 카카오 연결 해제 완료
- [ ] 개인정보 마스킹 처리
- [ ] soft delete로 복구 가능
- [ ] 테스트 코드 작성

---

## 추가 확인 필요 사항

- 탈퇴 후 데이터 보관 기간은?
- 탈퇴 사유 수집 여부?
- 재가입 제한 기간?

위 요구사항으로 진행할까요?
```

## 트리거 키워드

다음 키워드가 포함된 요청 시 이 스킬 활성화:
- "~해줘", "~추가", "~만들어", "~구현"
- "~수정", "~변경", "~고쳐"
- 복잡해 보이는 기능 요청
