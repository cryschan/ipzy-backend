# Cross Review 개선 사항

> 작성일: 2025-12-16
> 리뷰어: Claude + Codex (gpt-5.2)
> 관련 문서: `docs/plans/recommendation-retry-implementation-plan.md`

---

## 1. 추천 히스토리 페이지네이션 적용

### 우선순위: 중간

### 현재 상태
- `GET /api/recommendations/me` 전체 히스토리 반환
- 데이터 증가 시 성능 저하 가능

### 필요 작업

#### Controller 수정
```java
// RecommendationController.java
@GetMapping("/me")
public ApiResponse<Page<RecommendationSummaryResponse>> getMyRecommendations(
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable) {

    Long currentUserId = SecurityUtil.getCurrentUserIdOrThrow();
    Page<Recommendation> recommendations = recommendationService.getRecommendationsByUser(currentUserId, pageable);

    return ApiResponse.success(
            recommendations.map(RecommendationSummaryResponse::from)
    );
}
```

#### Service 수정
```java
// RecommendationService.java
public Page<Recommendation> getRecommendationsByUser(Long userId, Pageable pageable) {
    return recommendationRepository.findByUserIdWithItems(userId, pageable);
}
```

#### Repository 수정
```java
// RecommendationRepository.java
@Query("""
    SELECT DISTINCT r FROM Recommendation r
    LEFT JOIN FETCH r.items
    WHERE r.session.user.id = :userId
    ORDER BY r.createdAt DESC
    """)
Page<Recommendation> findByUserIdWithItems(@Param("userId") Long userId, Pageable pageable);
```

### 적용 시점
- MVP 이후, 사용자당 추천 10건 이상 쌓일 때

---

## 2. SecurityUtil 테스트 코드 추가

### 우선순위: 낮음

### 현재 상태
- `SecurityUtil` 클래스 테스트 없음
- `@Slf4j` 선언되었으나 미사용

### 필요 작업

#### 테스트 코드 작성
```java
// SecurityUtilTest.java
@ExtendWith(MockitoExtension.class)
class SecurityUtilTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증된 사용자 ID 조회 성공")
    void getCurrentUserId_success() {
        // Given
        CustomUserPrincipal principal = CustomUserPrincipal.builder()
                .userId(1L)
                .email("test@test.com")
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // When
        Optional<Long> userId = SecurityUtil.getCurrentUserId();

        // Then
        assertThat(userId).isPresent().contains(1L);
    }

    @Test
    @DisplayName("미인증 시 빈 Optional 반환")
    void getCurrentUserId_unauthenticated() {
        // When
        Optional<Long> userId = SecurityUtil.getCurrentUserId();

        // Then
        assertThat(userId).isEmpty();
    }

    @Test
    @DisplayName("미인증 시 AuthException 발생")
    void getCurrentUserIdOrThrow_unauthenticated() {
        // When & Then
        assertThatThrownBy(SecurityUtil::getCurrentUserIdOrThrow)
                .isInstanceOf(AuthException.class);
    }
}
```

#### @Slf4j 제거 (미사용)
```java
// SecurityUtil.java
// @Slf4j 제거 - 사용하지 않음
public final class SecurityUtil {
```

### 적용 시점
- 테스트 커버리지 향상 시

---

## 3. 미사용 DTO 정리

### 우선순위: 낮음

### 현재 상태
- `ItemPositionDto`: 선언만 존재, 사용처 없음
- `OutfitResultDto`: 선언만 존재, 사용처 없음

### 옵션

| 옵션 | 방법 | 장점 | 단점 |
|------|------|------|------|
| A | 삭제 | 코드 깔끔 | 나중에 다시 작성 |
| B | TODO 주석 추가 | 의도 명확 | 미사용 코드 존재 |

### 권장: 옵션 B

```java
// ItemPositionDto.java
/**
 * 아이템 위치 정보 DTO - Python FastAPI 응답용
 *
 * TODO: Phase 2 스타일보드 이미지 합성 기능에서 사용 예정
 * Python FastAPI의 /composite 응답에 포함될 위치 정보
 */
public record ItemPositionDto(
        Integer x,
        Integer y,
        Integer width,
        Integer height
) {}
```

```java
// OutfitResultDto.java
/**
 * 코디 추천 결과 DTO - Python FastAPI 응답용
 *
 * TODO: Phase 2 스타일보드 합성 결과 매핑에서 사용 예정
 * OutfitRecommendationDto의 result 필드에 매핑
 */
public record OutfitResultDto(...)
```

### 적용 시점
- 코드 정리 시

---

## 4. Race Condition 모니터링

### 우선순위: 낮음

### 현재 상태
- `generateRecommendation`에서 체크-저장 사이 경쟁 조건 가능
- `regenerate` 기능으로 인해 유니크 제약조건 적용 불가

### 분석

```java
// RecommendationService.java:52
if (recommendationRepository.existsBySessionId(sessionId)) {  // 체크
    throw RecommendationException.recommendationAlreadyExists(sessionId);
}
// ... 다른 요청이 끼어들 수 있음
return recommendationRepository.saveAll(recommendations);  // 저장
```

### 현실적 대안

1. **프론트엔드 더블 클릭 방지**
   - 버튼 비활성화 + 로딩 상태 표시
   - 가장 효과적인 방법

2. **중복 추천 허용**
   - 현재 `regenerate` 기능이 이미 중복 허용
   - 비즈니스 로직상 문제 없음

3. **모니터링 추가**
   - 동일 세션 중복 추천 로그 기록
   - 문제 발생 빈도 파악 후 대응

### 권장
- **현행 유지** + 프론트엔드 더블 클릭 방지
- 운영 중 문제 발생 시 재검토

---

## 체크리스트

- [ ] 추천 히스토리 페이지네이션 적용
- [ ] SecurityUtil 테스트 코드 추가
- [ ] SecurityUtil @Slf4j 제거
- [ ] ItemPositionDto TODO 주석 추가
- [ ] OutfitResultDto TODO 주석 추가
- [ ] 프론트엔드 더블 클릭 방지 요청

---

## 관련 문서

- [추천 재시도 구현 계획](/docs/plans/recommendation-retry-implementation-plan.md)
- [추천 토큰 시스템 설계](/docs/plans/recommendation-token-system-plan.md)
- [추천 API 구현](/docs/api/03-recommendation-api-implementation.md)
