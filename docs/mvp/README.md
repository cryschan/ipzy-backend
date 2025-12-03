# IPZY MVP 문서

## 개요

IPZY는 AI 기반 패션 코디 추천 서비스입니다. 사용자가 스타일 퀴즈를 풀면 AI가 맞춤형 코디를 추천해줍니다.

## MVP 핵심 가치

```
퀴즈 풀기 → AI 코디 추천 → 저장/구매
```

## 문서 목차

| 문서 | 설명 |
|------|------|
| [01-scope.md](./01-scope.md) | MVP 범위 정의 (포함/제외 기능) |
| [02-architecture.md](./02-architecture.md) | 아키텍처 및 기술 결정 사항 |
| [03-api-spec.md](./03-api-spec.md) | API 명세 |
| [04-implementation-plan.md](./04-implementation-plan.md) | 구현 계획 |
| [05-data-requirements.md](./05-data-requirements.md) | 데이터 요구사항 |

## 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Spring Boot 3.2, Java 21 |
| Database | PostgreSQL |
| ORM | Spring Data JPA |
| 인증 | OAuth2 + Session (카카오) |
| AI | Spring AI + OpenAI |
| 결제 | PortOne (MVP 이후) |

## 주요 결정 사항

- **인증**: OAuth2(카카오) + Session 조합 (원클릭 가입, 즉시 로그아웃 용이)
- **OAuth2 로드맵**: MVP 카카오 → Phase 2 네이버/구글 추가
- **비회원**: 퀴즈/상품 조회만 가능, 추천 저장은 로그인 필요
- **추천 생성**: Spring AI를 통한 OpenAI 연동
