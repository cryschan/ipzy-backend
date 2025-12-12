# Recommendation 테이블 Unique Constraint 추가

## 배경
- `RecommendationService`에서 `existsBySessionId()` 로 중복 검증 중
- 애플리케이션 레벨 검증은 race condition에 취약

## 작업 내용
```sql
ALTER TABLE recommendations
ADD CONSTRAINT uk_recommendations_session_id UNIQUE (session_id);
```

## 체크리스트
- [ ] Flyway 마이그레이션 스크립트 작성
- [ ] 기존 데이터 중복 여부 확인
- [ ] 테스트 환경 적용 후 검증
- [ ] PR 생성

## 우선순위
낮음 (현재 트래픽 적음, 기능 동작에는 문제 없음)
