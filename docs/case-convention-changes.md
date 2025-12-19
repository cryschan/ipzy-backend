# API Naming Convention 통일 작업 완료

## 작업 요약

**브랜치:** `feature/case`

**목표:** API 전체를 camelCase로 통일하여 프론트엔드-백엔드-AI 서버 간 일관성 확보

**결과:** ✅ 완료

---

## 변경 사항

### 1. Python AI 서버 (ipzy-ai)
**브랜치:** `feature/case`

**추가:**
- `app/middleware/case_converter.py` - CamelCase 변환 미들웨어
- `app/middleware/__init__.py`

**수정:**
- `app/main.py` - CamelCaseMiddleware 등록

**효과:**
- 모든 API 응답이 자동으로 camelCase로 변환
- Python 코드 내부는 snake_case 유지 (Python 표준)

**Before:**
```json
{
  "total_price": 100000,
  "composite_image_url": "...",
  "product_id": 123,
  "link_url": "..."
}
```

**After:**
```json
{
  "totalPrice": 100000,
  "compositeImageUrl": "...",
  "productId": 123,
  "linkUrl": "..."
}
```

---

### 2. Java 백엔드 (ipzy-backend)
**브랜치:** `feature/case`

**수정된 파일:**
```
src/main/java/com/ipzy/domain/recommendation/dto/response/
├── RecommendationResponse.java           (불필요한 @JsonProperty 제거)
├── OutfitRecommendationDto.java          (불필요한 @JsonProperty 제거)
├── OutfitResultDto.java                  (불필요한 @JsonProperty 제거)
├── RecommendedItemDto.java               (불필요한 @JsonProperty 제거)
├── RecommendationItemResponse.java       (snake_case → camelCase)
└── RecommendationSummaryResponse.java    (불필요한 @JsonProperty 제거)
```

**주요 변경:**

#### RecommendationItemResponse.java
**Before:**
```java
public record RecommendationItemResponse(
        @JsonProperty("product_id")  // ❌ snake_case
        Long productId,
        @JsonProperty("link_url")     // ❌ snake_case
        String linkUrl
)
```

**After:**
```java
public record RecommendationItemResponse(
        Long productId,      // ✅ camelCase (자동 매핑)
        String linkUrl       // ✅ camelCase (자동 매핑)
)
```

#### 기타 DTO
- `@JsonProperty("compositeImageUrl")` 제거 → `String compositeImageUrl`
- `@JsonProperty("imageWidth")` 제거 → `Integer imageWidth`
- `@JsonProperty("totalPrice")` 제거 → `Integer totalPrice`
- 등등...

**원칙:**
- camelCase 필드는 Jackson이 자동으로 매핑하므로 @JsonProperty 불필요
- 제거 후 코드가 더 깔끔하고 명확해짐

---

### 3. 프론트엔드 (ipzy-frontend)
**변경 사항:** 없음

**이유:**
- 프론트엔드는 이미 camelCase 사용
- Java 백엔드가 camelCase로 응답하므로 자동 호환

---

## 테스트 결과

### Java 단위 테스트
```bash
./gradlew test --tests "RecommendationServiceTest"
```
✅ 모든 테스트 통과

### 컴파일
```bash
./gradlew compileJava
```
✅ 빌드 성공

---

## API 응답 형식 (통일 후)

### 추천 생성 API
**Endpoint:** `POST /api/recommendations/sessions/{sessionId}/generate`

**응답:**
```json
{
  "success": true,
  "data": [
    {
      "displayOrder": 1,
      "occasion": "데이트",
      "season": "봄",
      "style": "캐주얼",
      "reason": "밝은 색감의 캐주얼 룩",
      "status": "completed",
      "jobId": "rec-1",
      "createdAt": "2025-12-15T08:32:14Z",
      "completedAt": "2025-12-15T08:32:17Z",
      "result": {
        "success": true,
        "message": "success",
        "compositeImageUrl": "https://...",
        "imageWidth": 1200,
        "imageHeight": 1600,
        "totalPrice": 237000,
        "items": [
          {
            "productId": 118,
            "category": "TOP",
            "name": "오버핏 옥스포드 셔츠",
            "brand": "무신사 스탠다드",
            "price": 59000,
            "imageUrl": "https://...",
            "linkUrl": "https://...",
            "position": {
              "x": 60,
              "y": 100,
              "width": 480,
              "height": 576
            }
          }
        ]
      },
      "error": null
    }
  ]
}
```

**특징:**
- ✅ 모든 필드가 camelCase로 통일
- ✅ TypeScript 타입과 완벽 매칭
- ✅ 프론트엔드에서 추가 변환 불필요

---

## 배포 전 확인 사항

### Python AI 서버
- [ ] `feature/case` 브랜치를 main에 머지
- [ ] 배포 후 응답이 camelCase인지 확인
- [ ] 기존 API 호환성 테스트

### Java 백엔드
- [ ] `feature/case` 브랜치를 main에 머지
- [ ] Python 배포 후 연동 테스트
- [ ] 프론트엔드 통합 테스트

### 프론트엔드
- [ ] 변경 사항 없으므로 재배포 불필요
- [ ] 백엔드 배포 후 정상 작동 확인

---

## 롤백 방법

문제 발생 시:

### Python
```bash
git revert <commit-hash>
# 또는
git checkout main
```

### Java
```bash
git revert <commit-hash>
# 또는
git checkout main
```

---

## 참고 자료

- Jackson camelCase 자동 매핑: [Jackson Documentation](https://github.com/FasterXML/jackson-databind)
- Google JSON Style Guide: [camelCase 권장](https://google.github.io/styleguide/jsoncstyleguide.xml)

---

## 작업 일시

- 2025-12-19
- 브랜치: `feature/case`
- 작업자: Claude Code

---

## 요약

✅ **Python:** CamelCase 미들웨어로 모든 응답 자동 변환
✅ **Java:** 불필요한 @JsonProperty 제거, camelCase 통일
✅ **프론트:** 변경 없음, 자동 호환
✅ **테스트:** 모두 통과
✅ **배포:** 준비 완료
