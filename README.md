# IPZY Backend

패션 스타일 퀴즈 기반 코디 추천 서비스 백엔드

## 기술 스택

- Java 21
- Spring Boot 3.2.0
- PostgreSQL 16
- Spring Security + Session
- Docker / Docker Compose

## 프로젝트 구조

```
src/main/java/com/ipzy/
├── domain/
│   ├── user/          # 사용자
│   ├── auth/          # 인증
│   ├── quiz/          # 스타일 퀴즈
│   ├── product/       # 상품/브랜드
│   ├── recommendation/# 코디 추천
│   ├── outfit/        # 저장된 코디
│   ├── subscription/  # 구독
│   ├── payment/       # 결제
│   ├── activity/      # 활동 로그
│   └── admin/         # 관리자
└── global/
    ├── config/        # 설정
    ├── common/        # 공용 클래스
    └── exception/     # 예외 처리
```

## 실행 방법

### Docker Compose (권장)

```bash
docker compose up
```

### 로컬 실행

```bash
# PostgreSQL 실행
docker compose up postgres

# 앱 실행
./gradlew bootRun
```

## 환경 변수

| 변수 | 설명 | 기본값 |
|-----|------|-------|
| DB_HOST | DB 호스트 | localhost |
| DB_PORT | DB 포트 | 5432 |
| DB_NAME | DB 이름 | ipzy |
| DB_USERNAME | DB 사용자 | postgres |
| DB_PASSWORD | DB 비밀번호 | postgres |

## API 문서

http://localhost:8080/swagger-ui/index.html
