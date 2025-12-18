# 퀴즈 도메인 엔티티 관계

> Quiz, Question, Option, Session, Answer 엔티티 간의 관계 및 설계 원칙

## 목차

1. [엔티티 관계도](#엔티티-관계도)
2. [계층 구조](#계층-구조)
3. [관계 상세](#관계-상세)
4. [생명주기 관리](#생명주기-관리)

---

## 엔티티 관계도

```
Quiz (1) ────< (N) QuizQuestion
  │                      │
  │                      └───< (N) QuizOption
  │
  └───< (N) QuizSession
            │
            └───< (N) QuizAnswer ────> (1) QuizQuestion
```

### 관계 요약

- **Quiz → QuizQuestion**: 1:N (OneToMany)
- **QuizQuestion → QuizOption**: 1:N (OneToMany)
- **Quiz → QuizSession**: 1:N (OneToMany)
- **QuizSession → QuizAnswer**: 1:N (OneToMany)
- **QuizQuestion → QuizAnswer**: 1:N (OneToMany)

---

## 계층 구조

### 1단계: 퀴즈 정의 계층

```
Quiz
  └── QuizQuestion (여러 개)
        └── QuizOption (여러 개)
```

**특징:**

- 퀴즈는 여러 질문을 포함
- 질문은 여러 옵션을 포함
- 계층적 구조로 퀴즈 템플릿 정의

### 2단계: 사용자 응답 계층

```
QuizSession (Quiz 참조)
  └── QuizAnswer (여러 개, QuizQuestion 참조)
```

**특징:**

- 세션은 특정 퀴즈에 속함
- 답변은 세션과 질문을 모두 참조
- 사용자의 실제 응답 데이터

---

## 관계 상세

### Quiz ↔ QuizQuestion

**관계:** 1:N (OneToMany)

- `CASCADE.ALL`, `orphanRemoval = true`: 퀴즈 삭제 시 질문도 함께 삭제
- `@OrderBy("displayOrder ASC")`: 표시 순서로 정렬
- `LAZY` 로딩

### QuizQuestion ↔ QuizOption

**관계:** 1:N (OneToMany)

- 질문 삭제 시 옵션도 함께 삭제
- `displayOrder` 순서로 정렬

### Quiz ↔ QuizSession

**관계:** 1:N (OneToMany)

- 세션은 반드시 퀴즈에 속함
- `user`는 nullable (비로그인 지원)
- 퀴즈 삭제 시 세션은 외래키 제약으로 인해 삭제 불가 (세션 먼저 삭제 필요)

### QuizSession ↔ QuizAnswer

**관계:** 1:N (OneToMany)

- 세션 삭제 시 답변도 함께 삭제
- 같은 질문에 대한 중복 답변 방지 (비즈니스 로직)

### QuizQuestion ↔ QuizAnswer

**관계:** 1:N (OneToMany)

- 답변은 반드시 질문에 속함
- `selectedOptions`에 `QuizOption.value` 저장
- 질문 삭제 시 답변은 외래키 제약으로 인해 삭제 불가 (답변 먼저 삭제 필요)

---

## 생명주기 관리

### Cascade 전파

- **Quiz 삭제**: QuizQuestion → QuizOption 자동 삭제
- **QuizSession 삭제**: QuizAnswer 자동 삭제

### Orphan Removal

- 질문/옵션/답변이 부모에서 제거되면 자동 삭제

### 주의사항

- 퀴즈/질문 삭제 시 세션/답변이 있으면 외래키 제약으로 삭제 불가
- 필요 시 세션/답변을 먼저 삭제하거나 소프트 삭제 고려

---

## 참고 사항

- **Fetch 전략**: 모든 관계는 `LAZY` 로딩, N+1 방지를 위해 `JOIN FETCH` 사용
- **정렬**: QuizQuestion, QuizOption은 `displayOrder ASC`로 정렬
- **비즈니스 규칙**: 활성화된 퀴즈만 세션 생성, 필수 질문 답변 필수, 답변 중복 방지
