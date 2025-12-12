# Java 설계/코드 참고 프로젝트 조사 보고서

> 작성일: 2025-12-13

## 1. 조사 목적

IPZY 백엔드 프로젝트의 설계와 코드 품질 향상을 위해 참고할 만한 Java/Spring Boot 오픈소스 프로젝트 조사

## 2. 조사 대상 프로젝트

### 2.1 Spring PetClinic (공식)

| 항목 | 내용 |
|------|------|
| GitHub | https://github.com/spring-projects/spring-petclinic |
| 기술 스택 | Java 17+, Spring Boot, Thymeleaf, Spring Data JPA |
| 아키텍처 | 전통적 3계층 (Controller → Service → Repository) |
| DB | H2 (기본), MySQL, PostgreSQL 선택 가능 |

**특징:**
- Spring 공식 레퍼런스로 가장 정석적인 패턴 제공
- 다양한 변형 존재 (Microservices, REST API, Cloud 버전)
- Testcontainers 기반 통합 테스트

### 2.2 RealWorld Conduit 구현체들

#### A. raeperd/realworld-springboot-java (코드 품질 중심)

| 항목 | 내용 |
|------|------|
| GitHub | https://github.com/raeperd/realworld-springboot-java |
| 기술 스택 | Java, Spring Boot, Spring Data JPA, Spring Security |
| 특징 | 불변성, 캡슐화, 테스트 커버리지 강조 |

**패키지 구조:**
```
src/
├── domain/          # 비즈니스 로직 (순수 POJO, Lombok 최소화)
├── infrastructure/  # 기술 구현 (JWT, DB 접근)
├── application/     # Spring 통합, 보안 로직
└── api/            # HTTP 컨트롤러
```

**핵심 설계 원칙:**
- `Always final whenever possible` - 모든 필드/변수를 가능하면 final로
- Package-private 클래스 최대화 - 불필요한 public 노출 방지
- 도메인에 Spring 의존성 최소화 - Entity에서 JPA 외 Spring 어노테이션 배제
- JWT를 서드파티 라이브러리 없이 자체 구현 (의존성 최소화)
- JaCoCo 기반 거의 100% 테스트 커버리지 목표

#### B. gothinkster/spring-boot-realworld-example-app (아키텍처 패턴)

| 항목 | 내용 |
|------|------|
| GitHub | https://github.com/gothinkster/spring-boot-realworld-example-app |
| 기술 스택 | Spring Boot, MyBatis, Spring Security |
| 아키텍처 | DDD + CQRS |

**4계층 구조:**
1. API 계층 - Spring MVC 웹 인터페이스
2. Core 계층 - 비즈니스 로직과 도메인 엔티티
3. Application 계층 - DTO 쿼리를 위한 고수준 서비스
4. Infrastructure 계층 - 기술적 구현 세부사항

**핵심 패턴:**
- DDD (Domain-Driven Design) - 비즈니스 용어와 인프라 용어 분리
- Data Mapper 패턴 - MyBatis로 구현
- CQRS - 읽기 모델과 쓰기 모델 분리

#### C. 1chz/realworld-java21-springboot3 (IPZY와 유사 스택)

| 항목 | 내용 |
|------|------|
| GitHub | https://github.com/1chz/realworld-java21-springboot3 |
| 기술 스택 | **Java 21, Spring Boot 3, JPA, Spring Security** |
| 아키텍처 | 모듈식 (core → persistence → api) |
| 인증 | JWT + OAuth 2.0 Resource Server |

**모듈 구조:**
```
module/
├── core/        # 도메인 모델 + 서비스 인터페이스
└── persistence/ # JPA 구현체

server/
└── api/         # Controller + DTO
```

**핵심: 의존성 역전 원칙(DIP)**
- core 모듈이 persistence에 의존하지 않음
- Repository 인터페이스는 core에, 구현체는 persistence에

#### D. sivaprasadreddy/spring-realworld-conduit-api (Spring Modulith)

| 항목 | 내용 |
|------|------|
| GitHub | https://github.com/sivaprasadreddy/spring-realworld-conduit-api |
| 기술 스택 | Java 21, Spring Boot, **Spring Modulith**, jOOQ, PostgreSQL |
| 아키텍처 | 도메인 기반 모듈화 |
| 테스트 | Testcontainers + PostgreSQL |

**특징:**
- Spring Modulith로 도메인별 모듈 분리
- jOOQ로 타입 안전 쿼리
- FlywayDB 마이그레이션

## 3. IPZY와 비교 분석

### 3.1 현재 IPZY 잘 되어있는 부분

| 항목 | 현재 상태 |
|------|----------|
| Service 구조 | `@Transactional(readOnly=true)` 기본 적용 |
| 예외 처리 | 도메인별 Exception + ErrorCode 패턴 |
| 전역 예외 핸들러 | `GlobalExceptionHandler` 잘 구성됨 |
| 엔티티 업데이트 | 도메인 메서드 (`user.updateProfile()`) 활용 |

### 3.2 참고 가능한 개선점

| 관점 | IPZY 현재 | 참고 가능 개선점 |
|------|-----------|-----------------|
| 패키지 구조 | `domain/{도메인}/` 평면 구조 | 모듈식 분리 (core/persistence/api) |
| 필드 불변성 | 일부 final 사용 | 가능한 모든 필드 final 적용 |
| 접근 제어 | public 위주 | package-private 활용 확대 |
| 테스트 | H2 인메모리 | Testcontainers 도입 고려 |
| 읽기/쓰기 분리 | 단일 Service | 복잡한 도메인에 CQRS 선택적 적용 |

## 4. 추천 참고 우선순위

| 순위 | 프로젝트 | 참고 포인트 |
|------|----------|------------|
| **1** | raeperd/realworld-springboot-java | 코드 품질, 테스트 전략, 캡슐화 |
| **2** | 1chz/realworld-java21-springboot3 | 동일 기술 스택, 모듈 구조 |
| **3** | gothinkster/spring-boot-realworld-example-app | CQRS, DDD 패턴 |
| **4** | sivaprasadreddy/spring-realworld-conduit-api | Spring Modulith, 고급 테스트 |

## 5. IPZY 적용 권장 사항

### 5.1 단기 적용 (낮은 비용)

1. **Package-private 활용 확대** - 불필요한 public 노출 줄이기
2. **final 키워드 적극 사용** - 필드, 지역 변수, 파라미터
3. **테스트 커버리지 강화** - JaCoCo 도입 고려

### 5.2 중기 적용 (중간 비용)

1. **복잡한 도메인에 읽기/쓰기 분리** - Quiz, Recommendation 등
2. **Testcontainers 도입** - 실제 PostgreSQL로 통합 테스트

### 5.3 장기 고려 (높은 비용)

1. **모듈 분리** - core/persistence/api 구조 (프로젝트 규모 증가 시)
2. **Spring Modulith** - 도메인 경계 명확화

## 6. 참고 링크

- [Spring PetClinic](https://github.com/spring-projects/spring-petclinic)
- [Spring PetClinic Community](https://spring-petclinic.github.io/)
- [RealWorld Example Apps](https://codebase.show/projects/realworld)
- [raeperd/realworld-springboot-java](https://github.com/raeperd/realworld-springboot-java)
- [gothinkster/spring-boot-realworld-example-app](https://github.com/gothinkster/spring-boot-realworld-example-app)
- [1chz/realworld-java21-springboot3](https://github.com/1chz/realworld-java21-springboot3)
- [sivaprasadreddy/spring-realworld-conduit-api](https://github.com/sivaprasadreddy/spring-realworld-conduit-api)
