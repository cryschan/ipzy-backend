# 룩(스타일)별 브랜드 정리

20-40대 남성을 위한 스타일별 대표 브랜드 정리

## 🎯 브랜드 타입 구분

### CLOTHING (의류 브랜드)
- 크롤링 소스: **무신사**
- 카테고리: TOP, BOTTOM, OUTER
- primaryStyle: 옷 스타일 (minimalist, hip_hop 등)

### SHOES (신발 브랜드)
- 크롤링 소스: **무신사 신발 랭킹/PLP**
- 카테고리: SHOES
- primaryStyle: 신발 타입 (sneakers, boots 등)

---

# 의류 브랜드 (CLOTHING)

## 1. 힙합 (Hip-Hop)
**특징**: 루즈한 핏, 오버사이즈, 스트릿 감성
**대표 브랜드**:
- 아웃스탠딩 (Outstanding)
- 드로우핏 (Draw Fit)

---

## 2. 미니멀 (Minimalist)
**특징**: 심플하고 깔끔한, 베이직, 무채색
**대표 브랜드**:
- 무신사 스탠다드 (Musinsa Standard)
- 디미트리블랙 (Dimitri Black)

---

## 3. 스트릿 (Street)
**특징**: 트렌디하고 개성있는, 스트릿 패션
**대표 브랜드**:
- 트릴리온 (Trillion)
- 에스피오나지 (Espionage)

---

## 4. 고프코어 (Gorpcore)
**특징**: 아웃도어, 기능성, 테크니컬
**대표 브랜드**:
- 노스페이스 (The North Face)
- 내셔널지오그래픽 (National Geographic)

---

## 5. 아메카지 (Amekaji / American Casual)
**특징**: 빈티지 아메리칸 캐주얼, 데님, 워크웨어
**대표 브랜드**:
- 프리즘웍스 (Frizmworks)
- 토피 (Toffee)

---

## 6. 시티보이 (Cityboy)
**특징**: 도회적이고 세련된, 트렌디
**대표 브랜드**:
- 디스이즈네버댓 (Thisisneverthat)
- 수아레 (Suare)

---

# 신발 브랜드 (SHOES)

## 1. 스니커즈 (Sneakers)
**특징**: 캐주얼, 스포티, 편안함
**대표 브랜드**:
- 나이키 (Nike)
- 아디다스 (Adidas)

**하위 분류 (subCategory)**:
- high_top (하이탑)
- low_top (로우탑)
- running (러닝화)
- basketball (농구화)

---

## 2. 로퍼 (Loafers)
**특징**: 세련되고 편안한, 끈 없는 구두
**대표 브랜드**:
- 구찌 (Gucci)
- 토즈 (Tod's)

---

## 3. 부츠 (Boots)
**특징**: 견고하고 실용적인, 가을/겨울 필수
**대표 브랜드**:
- 닥터마틴 (Dr. Martens)
- 팀버랜드 (Timberland)

**하위 분류 (subCategory)**:
- chelsea (첼시부츠)
- work (워크부츠)
- ugg (어그부츠)

---

## 4. 구두 (Dress Shoes)
**특징**: 포멀하고 단정한, 비즈니스
**대표 브랜드**:
- 클라크스 (Clarks)
- 콜한 (Cole Haan)

**하위 분류 (subCategory)**:
- derby (더비슈즈)
- oxford (옥스포드)
- monk (몽크스트랩)

---

## 5. 샌들 (Sandals)
**특징**: 시원하고 편안한, 여름 필수
**대표 브랜드**:
- 비르켄슈톡 (Birkenstock)
- 테바 (Teva)

---

## 데이터베이스 연동 참고

### 의류 브랜드 (CLOTHING)
`Brand` 엔티티의 `primaryStyle` 필드에 사용할 수 있는 값:
- `hip_hop`
- `minimalist`
- `street`
- `gorpcore`
- `amekaji`
- `cityboy`

### 신발 브랜드 (SHOES)
`Brand` 엔티티의 `primaryStyle` 필드에 사용할 수 있는 값:
- `sneakers`
- `loafers`
- `boots`
- `dress_shoes`
- `sandals`

### 예시 SQL

```sql
-- ==========================================
-- 의류 브랜드 (CLOTHING) - 무신사 크롤링
-- ==========================================

-- 힙합 스타일 브랜드
INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Outstanding', NULL, 'hip_hop', 'CLOTHING', NOW(), NOW());

INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Draw Fit', NULL, 'hip_hop', 'CLOTHING', NOW(), NOW());

-- 미니멀 스타일 브랜드
INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Musinsa Standard', NULL, 'minimalist', 'CLOTHING', NOW(), NOW());

INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Dimitri Black', NULL, 'minimalist', 'CLOTHING', NOW(), NOW());

-- 스트릿 스타일 브랜드
INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Trillion', NULL, 'street', 'CLOTHING', NOW(), NOW());

INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Espionage', NULL, 'street', 'CLOTHING', NOW(), NOW());

-- 고프코어 스타일 브랜드
INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('The North Face', NULL, 'gorpcore', 'CLOTHING', NOW(), NOW());

INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('National Geographic', NULL, 'gorpcore', 'CLOTHING', NOW(), NOW());

-- 아메카지 스타일 브랜드
INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Frizmworks', NULL, 'amekaji', 'CLOTHING', NOW(), NOW());

INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Toffee', NULL, 'amekaji', 'CLOTHING', NOW(), NOW());

-- 시티보이 스타일 브랜드
INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Thisisneverthat', NULL, 'cityboy', 'CLOTHING', NOW(), NOW());

INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Suare', NULL, 'cityboy', 'CLOTHING', NOW(), NOW());

-- ==========================================
-- 신발 브랜드 (SHOES) - 무신사 신발 랭킹/PLP
-- ==========================================

-- 스니커즈 브랜드
INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Nike', NULL, 'sneakers', 'SHOES', NOW(), NOW());

INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Adidas', NULL, 'sneakers', 'SHOES', NOW(), NOW());

-- 로퍼 브랜드
INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Gucci', NULL, 'loafers', 'SHOES', NOW(), NOW());

INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Tod''s', NULL, 'loafers', 'SHOES', NOW(), NOW());

-- 부츠 브랜드
INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Dr. Martens', NULL, 'boots', 'SHOES', NOW(), NOW());

INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Timberland', NULL, 'boots', 'SHOES', NOW(), NOW());

-- 구두 브랜드
INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Clarks', NULL, 'dress_shoes', 'SHOES', NOW(), NOW());

INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Cole Haan', NULL, 'dress_shoes', 'SHOES', NOW(), NOW());

-- 샌들 브랜드
INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Birkenstock', NULL, 'sandals', 'SHOES', NOW(), NOW());

INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at)
VALUES ('Teva', NULL, 'sandals', 'SHOES', NOW(), NOW());
```
