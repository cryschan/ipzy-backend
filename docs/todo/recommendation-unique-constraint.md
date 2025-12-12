# Recommendation 테이블 중복 생성 방지

## 배경
- `RecommendationService`에서 `existsBySessionId()`로 중복 검증 중
- 애플리케이션 레벨 검증은 race condition에 취약

## 현재 데이터 모델

```
QuizSession (1) ──── (N) Recommendation ──── (N) RecommendationItem
                     └─ displayOrder로 구분
```

- 하나의 세션에 **여러 Recommendation 행**이 저장됨 (코디별 1행)
- `displayOrder`로 순서 구분

## 작업 내용

### 제약 조건 설계

~~기존 (잘못됨)~~:
```sql
-- session_id UNIQUE는 현재 모델과 충돌 (세션당 여러 행 존재)
ALTER TABLE recommendations
ADD CONSTRAINT uk_recommendations_session_id UNIQUE (session_id);
```

**수정안 A: 복합 유니크 (권장)**
```sql
ALTER TABLE recommendations
ADD CONSTRAINT uk_recommendations_session_display_order
UNIQUE (session_id, display_order);
```

**수정안 B: 별도 "추천 묶음" 테이블**
- 장점: 세션-추천 1:1 관계 명확
- 단점: 스키마 변경 범위 큼

### 예외 처리

```java
// RecommendationService.java
try {
    recommendationRepository.saveAll(recommendations);
} catch (DataIntegrityViolationException e) {
    // 409 Conflict 반환 또는 기존 추천 반환
    log.warn("중복 추천 생성 시도: sessionId={}", sessionId);
    return recommendationRepository.findBySessionIdWithItems(sessionId);
}
```

### 기존 데이터 정리 (마이그레이션 전 실행)

```sql
-- 중복 데이터 확인
SELECT session_id, display_order, COUNT(*)
FROM recommendations
GROUP BY session_id, display_order
HAVING COUNT(*) > 1;

-- 중복 제거 (최신 것만 유지)
DELETE FROM recommendations r1
WHERE EXISTS (
    SELECT 1 FROM recommendations r2
    WHERE r1.session_id = r2.session_id
      AND r1.display_order = r2.display_order
      AND r1.id < r2.id
);
```

## 체크리스트

- [ ] 기존 데이터 중복 여부 확인 (위 SQL 실행)
- [ ] 중복 데이터 정리 SQL 실행
- [ ] Flyway 마이그레이션 스크립트 작성 (복합 유니크)
- [ ] `DataIntegrityViolationException` 처리 로직 추가
- [ ] 테스트 환경 적용 후 검증
- [ ] PR 생성

## 우선순위

**중간** (기존 "낮음"에서 상향)
- race condition으로 인한 데이터 중복 가능성 존재
- 외부 API 재시도/중복 호출 시 문제 발생 가능
