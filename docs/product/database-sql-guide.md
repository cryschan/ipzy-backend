# 데이터베이스 SQL 가이드

상품 및 브랜드 관련 데이터베이스 조작 SQL 모음

---

## 📋 목차

1. [브랜드 등록 SQL](#브랜드-등록-sql)
2. [상품 삭제 SQL](#상품-삭제-sql)
3. [데이터 조회 SQL](#데이터-조회-sql)

---

## 브랜드 등록 SQL

### 의류 브랜드 (CLOTHING) - 무신사 크롤링

```sql
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
```

### 신발 브랜드 (SHOES) - 무신사 신발 랭킹/PLP

```sql
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

### 브랜드 한번에 등록 (전체)

```sql
-- 의류 브랜드 12개 + 신발 브랜드 10개 = 총 22개
INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at) VALUES
('Outstanding', NULL, 'hip_hop', 'CLOTHING', NOW(), NOW()),
('Draw Fit', NULL, 'hip_hop', 'CLOTHING', NOW(), NOW()),
('Musinsa Standard', NULL, 'minimalist', 'CLOTHING', NOW(), NOW()),
('Dimitri Black', NULL, 'minimalist', 'CLOTHING', NOW(), NOW()),
('Trillion', NULL, 'street', 'CLOTHING', NOW(), NOW()),
('Espionage', NULL, 'street', 'CLOTHING', NOW(), NOW()),
('The North Face', NULL, 'gorpcore', 'CLOTHING', NOW(), NOW()),
('National Geographic', NULL, 'gorpcore', 'CLOTHING', NOW(), NOW()),
('Frizmworks', NULL, 'amekaji', 'CLOTHING', NOW(), NOW()),
('Toffee', NULL, 'amekaji', 'CLOTHING', NOW(), NOW()),
('Thisisneverthat', NULL, 'cityboy', 'CLOTHING', NOW(), NOW()),
('Suare', NULL, 'cityboy', 'CLOTHING', NOW(), NOW()),
('Nike', NULL, 'sneakers', 'SHOES', NOW(), NOW()),
('Adidas', NULL, 'sneakers', 'SHOES', NOW(), NOW()),
('Gucci', NULL, 'loafers', 'SHOES', NOW(), NOW()),
('Tod''s', NULL, 'loafers', 'SHOES', NOW(), NOW()),
('Dr. Martens', NULL, 'boots', 'SHOES', NOW(), NOW()),
('Timberland', NULL, 'boots', 'SHOES', NOW(), NOW()),
('Clarks', NULL, 'dress_shoes', 'SHOES', NOW(), NOW()),
('Cole Haan', NULL, 'dress_shoes', 'SHOES', NOW(), NOW()),
('Birkenstock', NULL, 'sandals', 'SHOES', NOW(), NOW()),
('Teva', NULL, 'sandals', 'SHOES', NOW(), NOW());
```

---

## 상품 삭제 SQL

### 상품만 전체 삭제 (브랜드는 유지)

```sql
-- 방법 1: DELETE (트랜잭션 롤백 가능)
DELETE FROM products;

-- 방법 2: TRUNCATE (더 빠름, AUTO_INCREMENT 초기화)
TRUNCATE TABLE products;
```

### 특정 브랜드의 상품만 삭제

```sql
-- 브랜드 ID로 삭제
DELETE FROM products WHERE brand_id = 1;

-- 브랜드명으로 삭제 (JOIN 사용)
DELETE FROM products
WHERE brand_id IN (SELECT id FROM brands WHERE name = 'Nike');
```

### 특정 카테고리 상품 삭제

```sql
-- TOP 카테고리 상품만 삭제
DELETE FROM products WHERE category = 'TOP';

-- 여러 카테고리 동시 삭제
DELETE FROM products WHERE category IN ('TOP', 'BOTTOM');
```

### 상품 + 브랜드 모두 삭제

```sql
-- 방법 1: 순서대로 삭제 (FK 제약조건 고려)
DELETE FROM products;  -- 먼저 상품 삭제
DELETE FROM brands;    -- 그 다음 브랜드 삭제

-- 방법 2: TRUNCATE (CASCADE 옵션)
TRUNCATE TABLE products CASCADE;
TRUNCATE TABLE brands CASCADE;
```

### 비활성화된 상품만 삭제

```sql
-- is_active가 false인 상품만 삭제
DELETE FROM products WHERE is_active = false;
```

---

## 데이터 조회 SQL

### 브랜드 조회

```sql
-- 전체 브랜드 조회
SELECT * FROM brands ORDER BY brand_type, primary_style;

-- 의류 브랜드만 조회
SELECT * FROM brands WHERE brand_type = 'CLOTHING';

-- 신발 브랜드만 조회
SELECT * FROM brands WHERE brand_type = 'SHOES';

-- 특정 스타일 브랜드 조회
SELECT * FROM brands WHERE primary_style = 'minimalist';

-- 브랜드별 상품 수 조회
SELECT b.name, b.brand_type, b.primary_style, COUNT(p.id) as product_count
FROM brands b
LEFT JOIN products p ON b.id = p.brand_id
GROUP BY b.id, b.name, b.brand_type, b.primary_style
ORDER BY product_count DESC;
```

### 상품 조회

```sql
-- 전체 상품 수 조회
SELECT COUNT(*) FROM products;

-- 브랜드별 상품 조회
SELECT p.*, b.name as brand_name
FROM products p
JOIN brands b ON p.brand_id = b.id
WHERE b.name = 'Nike';

-- 카테고리별 상품 수
SELECT category, COUNT(*) as count
FROM products
GROUP BY category
ORDER BY count DESC;

-- 스타일별 상품 수
SELECT primary_style, COUNT(*) as count
FROM products
GROUP BY primary_style
ORDER BY count DESC;

-- 가격대별 상품 조회
SELECT
    CASE
        WHEN price < 50000 THEN '5만원 미만'
        WHEN price < 100000 THEN '5만원~10만원'
        WHEN price < 200000 THEN '10만원~20만원'
        ELSE '20만원 이상'
    END as price_range,
    COUNT(*) as count
FROM products
GROUP BY price_range
ORDER BY MIN(price);

-- 최근 등록된 상품 조회
SELECT p.*, b.name as brand_name
FROM products p
JOIN brands b ON p.brand_id = b.id
ORDER BY p.created_at DESC
LIMIT 10;
```

### 통계 조회

```sql
-- 브랜드 타입별 통계
SELECT
    b.brand_type,
    COUNT(DISTINCT b.id) as brand_count,
    COUNT(p.id) as product_count,
    AVG(p.price) as avg_price
FROM brands b
LEFT JOIN products p ON b.id = p.brand_id
GROUP BY b.brand_type;

-- 스타일별 통계
SELECT
    primary_style,
    COUNT(*) as product_count,
    MIN(price) as min_price,
    MAX(price) as max_price,
    AVG(price) as avg_price
FROM products
GROUP BY primary_style
ORDER BY product_count DESC;
```

---

## 데이터 검증 SQL

### 중복 체크

```sql
-- 중복 브랜드명 체크
SELECT name, brand_type, COUNT(*) as count
FROM brands
GROUP BY name, brand_type
HAVING COUNT(*) > 1;

-- 중복 상품명 체크 (같은 브랜드 내)
SELECT p1.name, b.name as brand_name, COUNT(*) as count
FROM products p1
JOIN brands b ON p1.brand_id = b.id
GROUP BY p1.brand_id, p1.name, b.name
HAVING COUNT(*) > 1;
```

### 데이터 무결성 체크

```sql
-- brand_id가 없는 고아 상품 체크
SELECT * FROM products
WHERE brand_id NOT IN (SELECT id FROM brands);

-- 필수 필드 누락 체크
SELECT * FROM products
WHERE name IS NULL
   OR price IS NULL
   OR price <= 0
   OR category IS NULL;

-- 비정상 가격 체크
SELECT * FROM products
WHERE price > original_price;
```

---

## 데이터 업데이트 SQL

### 브랜드 정보 수정

```sql
-- 브랜드 스타일 변경
UPDATE brands
SET primary_style = 'minimalist', modified_at = NOW()
WHERE name = 'Musinsa Standard';

-- 브랜드 로고 추가
UPDATE brands
SET logo_url = 'https://example.com/logo.png', modified_at = NOW()
WHERE name = 'Nike';
```

### 상품 정보 일괄 수정

```sql
-- 특정 브랜드 상품 가격 10% 인상
UPDATE products
SET price = price * 1.1, modified_at = NOW()
WHERE brand_id = (SELECT id FROM brands WHERE name = 'Nike');

-- 모든 상품 활성화
UPDATE products
SET is_active = true, modified_at = NOW();

-- 특정 카테고리 상품 비활성화
UPDATE products
SET is_active = false, modified_at = NOW()
WHERE category = 'ACCESSORY';
```

---

## 참고사항

### 브랜드 타입
- `CLOTHING`: 의류 브랜드 (무신사 크롤링)
- `SHOES`: 신발 브랜드 (무신사 신발 랭킹/PLP)

### Primary Style 값

**의류 (CLOTHING)**:
- `hip_hop`: 힙합 (루즈한 핏, 오버사이즈)
- `minimalist`: 미니멀 (심플, 베이직, 무채색)
- `street`: 스트릿 (트렌디, 개성있는)
- `gorpcore`: 고프코어 (아웃도어, 기능성)
- `amekaji`: 아메카지 (빈티지 아메리칸 캐주얼)
- `cityboy`: 시티보이 (도회적, 세련된)

**신발 (SHOES)**:
- `sneakers`: 스니커즈 (캐주얼, 스포티)
- `loafers`: 로퍼 (세련되고 편안한)
- `boots`: 부츠 (견고하고 실용적)
- `dress_shoes`: 구두 (포멀, 비즈니스)
- `sandals`: 샌들 (시원하고 편안한)

### 카테고리
- `TOP`: 상의
- `BOTTOM`: 하의
- `OUTER`: 아우터
- `SHOES`: 신발
- `ACCESSORY`: 액세서리

### 주의사항
1. **TRUNCATE vs DELETE**
   - `TRUNCATE`: 더 빠르지만 롤백 불가, AUTO_INCREMENT 초기화
   - `DELETE`: 느리지만 트랜잭션 롤백 가능

2. **FK 제약조건**
   - 상품 삭제 시 브랜드는 유지됨
   - 브랜드 삭제 시 해당 브랜드의 상품을 먼저 삭제해야 함

3. **배치 INSERT**
   - 여러 브랜드를 한번에 등록할 때는 VALUES를 콤마로 연결하면 더 효율적

4. **인덱스 고려**
   - 대량의 데이터 삭제 후 인덱스 재구성 필요할 수 있음
   - `ANALYZE TABLE products;` 또는 `REINDEX;` 실행 권장
