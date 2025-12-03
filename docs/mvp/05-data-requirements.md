# 데이터 요구사항

## 1. 퀴즈 데이터

### 초기 퀴즈 구조

MVP에서는 **1개의 퀴즈**로 시작합니다.

```
퀴즈: "나의 스타일 찾기"
├── 질문 1: 선호하는 스타일 (단일 선택)
├── 질문 2: 주로 입는 상황 (복수 선택)
├── 질문 3: 선호하는 핏 (단일 선택)
├── 질문 4: 선호하는 색상 계열 (복수 선택)
├── 질문 5: 선호하는 계절감 (복수 선택)
├── 질문 6: 체형 고민 (복수 선택)
├── 질문 7: 예산 범위 (단일 선택)
├── 질문 8: 선호하는 브랜드 느낌 (단일 선택)
├── 질문 9: 피하고 싶은 스타일 (복수 선택)
└── 질문 10: 최근 관심 있는 아이템 (복수 선택)
```

### 질문 상세

#### 질문 1: 선호하는 스타일

| 옵션 | value | 설명 |
|------|-------|------|
| 캐주얼 | casual | 편안하고 일상적인 |
| 미니멀 | minimal | 깔끔하고 단순한 |
| 스트릿 | street | 트렌디하고 개성있는 |
| 포멀 | formal | 단정하고 격식있는 |
| 로맨틱 | romantic | 여성스럽고 부드러운 |

#### 질문 2: 주로 입는 상황

| 옵션 | value |
|------|-------|
| 출퇴근/통학 | commute |
| 데이트 | date |
| 친구 만남 | friends |
| 여행 | travel |
| 재택/집콕 | home |

#### 질문 3: 선호하는 핏

| 옵션 | value |
|------|-------|
| 오버핏 | overfit |
| 레귤러핏 | regular |
| 슬림핏 | slim |

#### 질문 4: 선호하는 색상 계열

| 옵션 | value |
|------|-------|
| 모노톤 (블랙/화이트/그레이) | monotone |
| 뉴트럴 (베이지/브라운/카키) | neutral |
| 파스텔 | pastel |
| 비비드 (원색) | vivid |
| 어스톤 (자연색) | earth |

#### 질문 5: 선호하는 계절감

| 옵션 | value |
|------|-------|
| 봄 | spring |
| 여름 | summer |
| 가을 | fall |
| 겨울 | winter |

#### 질문 6: 체형 고민 (선택사항)

| 옵션 | value |
|------|-------|
| 어깨가 좁음 | narrow_shoulder |
| 어깨가 넓음 | wide_shoulder |
| 상체가 긺 | long_torso |
| 하체가 긺 | long_legs |
| 특별히 없음 | none |

#### 질문 7: 예산 범위 (코디 전체)

| 옵션 | value | 범위 |
|------|-------|------|
| 10만원 이하 | budget_low | ~100,000 |
| 10-20만원 | budget_mid | 100,000~200,000 |
| 20-30만원 | budget_high | 200,000~300,000 |
| 30만원 이상 | budget_premium | 300,000~ |

#### 질문 8: 선호하는 브랜드 느낌

| 옵션 | value |
|------|-------|
| 가성비 좋은 SPA | spa |
| 디자이너 감성 | designer |
| 스포츠/아웃도어 | sports |
| 빈티지/유니크 | vintage |

#### 질문 9: 피하고 싶은 스타일

| 옵션 | value |
|------|-------|
| 너무 화려한 | too_flashy |
| 너무 단조로운 | too_plain |
| 타이트한 핏 | tight_fit |
| 루즈한 핏 | loose_fit |
| 특별히 없음 | none |

#### 질문 10: 최근 관심 있는 아이템

| 옵션 | value |
|------|-------|
| 아우터 (자켓/코트) | outer |
| 니트/스웨터 | knit |
| 데님 | denim |
| 슬랙스 | slacks |
| 스니커즈 | sneakers |
| 가방/액세서리 | accessory |

---

## 2. 상품 데이터

### MVP 최소 상품 수

| 카테고리 | 최소 수량 | 비고 |
|---------|----------|------|
| TOP | 20개 | 셔츠, 티셔츠, 니트 등 |
| BOTTOM | 15개 | 팬츠, 스커트 등 |
| OUTER | 10개 | 자켓, 코트 등 |
| SHOES | 10개 | 스니커즈, 로퍼 등 |
| ACCESSORY | 5개 | 가방, 모자 등 (선택) |
| **합계** | **60개+** | |

### 상품 데이터 형식

```json
{
  "name": "오버핏 옥스포드 셔츠",
  "brandName": "무신사 스탠다드",
  "category": "TOP",
  "subCategory": "셔츠",
  "price": 45900,
  "originalPrice": 59000,
  "imageUrl": "https://...",
  "images": ["https://...", "https://..."],
  "sizes": ["S", "M", "L", "XL"],
  "colors": ["화이트", "블루", "핑크"],
  "tags": ["캐주얼", "오버핏", "봄", "가을"],
  "purchaseUrl": "https://..."
}
```

### 상품 데이터 소스 옵션

| 옵션 | 장점 | 단점 |
|------|------|------|
| **수동 입력** | 품질 보장 | 시간 소요 |
| **크롤링** | 대량 수집 | 법적 이슈, 유지보수 |
| **제휴 API** | 안정적 | 계약 필요 |
| **목업 데이터** | 빠른 시작 | 실제 서비스 불가 |

**MVP 추천**: 수동 입력 (60개) + 실제 구매 링크

---

## 3. AI 프롬프트 설계

### 시스템 프롬프트

```
당신은 전문 패션 코디네이터입니다.
사용자의 스타일 분석 결과와 가용 상품 목록을 바탕으로
최적의 코디를 추천해주세요.

규칙:
1. 각 코디는 반드시 TOP, BOTTOM, SHOES를 포함해야 합니다.
2. OUTER와 ACCESSORY는 선택사항입니다.
3. 총 3개의 코디를 추천해주세요.
4. 각 코디의 총 가격이 사용자의 예산 범위 내에 있어야 합니다.
5. 사용자가 피하고 싶다고 한 스타일은 제외해주세요.
6. 각 코디에 대해 추천 이유를 설명해주세요.
```

### 사용자 프롬프트 템플릿

```
## 사용자 스타일 분석

선호 스타일: {style}
주요 상황: {occasions}
선호 핏: {fit}
선호 색상: {colors}
선호 계절: {seasons}
체형 고민: {bodytype}
예산: {budget}
브랜드 느낌: {brand_feel}
피하고 싶은 스타일: {avoid}
관심 아이템: {interests}

## 가용 상품 목록

### TOP
{top_products}

### BOTTOM
{bottom_products}

### OUTER
{outer_products}

### SHOES
{shoes_products}

### ACCESSORY
{accessory_products}

## 출력 형식

다음 JSON 형식으로 3개의 코디를 추천해주세요:

```json
{
  "recommendations": [
    {
      "displayOrder": 1,
      "style": "캐주얼 시크",
      "occasion": "데일리",
      "season": "봄/가을",
      "reason": "추천 이유 설명...",
      "items": [
        {"productId": 101, "category": "TOP"},
        {"productId": 102, "category": "BOTTOM"},
        {"productId": 103, "category": "SHOES"}
      ]
    }
  ]
}
```
```

### 응답 파싱

```java
@Data
public class AiRecommendationResponse {
    private List<RecommendationResult> recommendations;
}

@Data
public class RecommendationResult {
    private Integer displayOrder;
    private String style;
    private String occasion;
    private String season;
    private String reason;
    private List<ItemResult> items;
}

@Data
public class ItemResult {
    private Long productId;
    private String category;
}
```

---

## 4. 데이터 마이그레이션

### Flyway 스크립트

```
resources/db/migration/
├── V1__create_tables.sql        # 테이블 생성 (기존)
├── V2__insert_quiz_data.sql     # 퀴즈 초기 데이터
├── V3__insert_brand_data.sql    # 브랜드 데이터
└── V4__insert_product_data.sql  # 상품 데이터
```

### V2__insert_quiz_data.sql

```sql
-- 퀴즈
INSERT INTO quizzes (title, description, is_active, display_order, created_at, modified_at)
VALUES ('나의 스타일 찾기', '10가지 질문으로 당신의 스타일을 분석합니다', true, 1, NOW(), NOW());

-- 질문 1
INSERT INTO quiz_questions (quiz_id, text, type, display_order, required, created_at, modified_at)
VALUES (1, '선호하는 스타일은?', 'SINGLE', 1, true, NOW(), NOW());

-- 질문 1 옵션
INSERT INTO quiz_options (question_id, text, value, image_url, display_order, created_at, modified_at)
VALUES
(1, '캐주얼', 'casual', NULL, 1, NOW(), NOW()),
(1, '미니멀', 'minimal', NULL, 2, NOW(), NOW()),
(1, '스트릿', 'street', NULL, 3, NOW(), NOW()),
(1, '포멀', 'formal', NULL, 4, NOW(), NOW()),
(1, '로맨틱', 'romantic', NULL, 5, NOW(), NOW());

-- ... 나머지 질문들
```

---

## 5. 체크리스트

### 퀴즈 데이터
- [ ] 퀴즈 1개 생성
- [ ] 질문 10개 생성
- [ ] 각 질문별 옵션 생성
- [ ] 이미지 URL (선택)

### 상품 데이터
- [ ] 브랜드 5개+ 등록
- [ ] TOP 20개 등록
- [ ] BOTTOM 15개 등록
- [ ] OUTER 10개 등록
- [ ] SHOES 10개 등록
- [ ] 구매 링크 확인

### AI 프롬프트
- [ ] 시스템 프롬프트 작성
- [ ] 사용자 프롬프트 템플릿
- [ ] 응답 파싱 로직
- [ ] 에러 처리 (파싱 실패 등)

---

## 6. 향후 확장

### 퀴즈 확장
- 상황별 퀴즈 (출근룩, 데이트룩 등)
- 계절별 퀴즈
- 특별 이벤트 퀴즈

### 상품 확장
- 크롤링 자동화
- 제휴 API 연동
- 가격 변동 추적

### AI 개선
- 사용자 피드백 학습
- 추천 정확도 향상
- 개인화 강화
