# 퀴즈 도메인 구조

> 사용자 스타일 선호도를 파악하기 위한 퀴즈 시스템의 구조와 설계

## 목차

1. [도메인 개요](#도메인-개요)
2. [폴더 구조](#폴더-구조)
3. [엔티티 구조](#엔티티-구조)
4. [계층 구조](#계층-구조)
5. [예외 처리](#예외-처리)
6. [관련 문서](#관련-문서)

---

## 도메인 개요

퀴즈 도메인은 사용자의 스타일 선호도를 파악하기 위한 설문 시스템입니다. 사용자는 여러 질문에 답변하고, 완료된 퀴즈 세션은 AI 추천 시스템의 입력으로 사용됩니다.

### 주요 특징

- **비로그인 지원**: 로그인하지 않은 사용자도 퀴즈를 풀 수 있음
- **익명 세션**: 비로그인 상태에서 퀴즈를 풀고, 로그인 후 추천을 받을 때 세션을 사용자에게 연결
- **유연한 질문 타입**: 단일 선택(SINGLE), 다중 선택(MULTIPLE) 지원
- **필수/선택 질문**: 질문별로 필수 여부 설정 가능
- **진행 상태 관리**: 세션별 진행 상태 조회 및 완료 처리

---

## 폴더 구조

```
src/main/java/com/ipzy/domain/quiz/
├── config/
│   ├── QuizDataInitializer.java
│   └── QuizSessionDataInitializer.java
├── controller/
│   ├── QuizController.java
│   └── QuizSessionController.java
├── dto/
│   ├── QuizAnswerProgressResponse.java
│   ├── QuizAnswerRequest.java
│   ├── QuizAnswerResponse.java
│   ├── QuizCompletionResponse.java
│   ├── QuizOptionResponse.java
│   ├── QuizQuestionResponse.java
│   ├── QuizSessionProgressResponse.java
│   └── QuizSessionStartResponse.java
├── entity/
│   ├── Quiz.java
│   ├── QuizAnswer.java
│   ├── QuizOption.java
│   ├── QuizQuestion.java
│   └── QuizSession.java
├── exception/
│   ├── QuizErrorCode.java
│   └── QuizException.java
├── repository/
│   ├── QuizAnswerRepository.java
│   ├── QuizQuestionRepository.java
│   ├── QuizRepository.java
│   └── QuizSessionRepository.java
└── service/
    └── QuizService.java
```

---

## 엔티티 구조

### 주요 엔티티

- **Quiz**: 퀴즈 최상위 엔티티, 활성화 여부 관리
- **QuizQuestion**: 퀴즈에 속한 질문, SINGLE/MULTIPLE 타입 지원, 필수/선택 질문 구분
- **QuizOption**: 질문의 선택지, `text`(표시)와 `value`(저장값) 구분
- **QuizSession**: 사용자의 퀴즈 세션, 비로그인 지원 (`user` nullable)
- **QuizAnswer**: 사용자의 답변, `selectedOptions`에 옵션 값 저장 (JSONB)

자세한 관계는 [엔티티 관계 문서](./03-quiz-entity-relationships.md) 참조.

---

## 계층 구조

- **Controller**: `QuizController`, `QuizSessionController` - API 엔드포인트 제공
- **Service**: `QuizService` - 비즈니스 로직 및 검증 처리
- **Repository**: 각 엔티티별 Repository - 데이터 접근, JOIN FETCH 활용

---

## 예외 처리

- **QuizException**: 퀴즈 도메인 전용 예외 클래스
- **QuizErrorCode**: 퀴즈 관련 에러 코드 (QUIZ_001 ~ QUIZ_017)
  - 퀴즈/세션/질문/답변/옵션/검증/초기화 관련 에러 코드 정의

---

## 관련 문서

- [비즈니스 로직 및 추천 연동](./02-quiz-business-logic-and-integration.md): 핵심 비즈니스 로직과 추천 도메인 연동 방식
- [엔티티 관계](./03-quiz-entity-relationships.md): Quiz, Question, Option, Session, Answer 엔티티 간의 관계 및 설계 원칙
- [세션 생명주기 정책](./04-quiz-session-lifecycle.md): QuizSession 상태 전이, 수명 관리, 비회원 처리, 중복 세션 허용 정책
- [답변 저장 및 검증 전략](./05-quiz-answer-validation.md): QuizAnswer 저장 전략과 도메인 무결성 검증 규칙

---

## 참고 사항

- 모든 퀴즈 API는 비로그인 사용자도 접근 가능
- `JOIN FETCH`를 사용하여 N+1 문제 방지
