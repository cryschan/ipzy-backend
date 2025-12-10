# 상품 크롤링 가이드

Java 기반 자동 크롤링 시스템 사용 가이드

## 📋 개요

20-40대 남성을 위한 의류 및 신발 상품을 자동으로 크롤링하여 DB에 저장하는 시스템입니다.

### 크롤링 소스
- **무신사 PLP API**: 의류/신발 브랜드
- **무신사 신발 랭킹 API**: 신발 카테고리별 랭킹

### 주요 기능
- ✅ Java 기반 (무신사 API 사용)
- ✅ REST API로 크롤링 실행
- ✅ **배치 중복 체크** (성능: N+1 → 2 쿼리)
- ✅ **트랜잭션 분리** (크롤링 vs 저장)
- ✅ **실패 목록 반환** (상품명, 브랜드명, 사유)
- ✅ 브랜드/스타일별 크롤링
- ✅ **시즌별 상품 관리** (SS/FW)
- ✅ 신발 랭킹 크롤링 시 브랜드 자동 생성

---

## 🚀 API 엔드포인트

### 1. 전체 브랜드 크롤링

DB에 등록된 모든 브랜드의 상품을 크롤링합니다.

```bash
POST /api/admin/crawling/products/all?limit=3
```

**파라미터:**
- `limit`: 브랜드당 크롤링 개수 (기본값: 3)
  - 의류: `limit × 3`개 (상의/아우터/하의 각 limit개)
  - 신발: `limit`개

**응답:**
```json
{
  "success": true,
  "data": {
    "savedCount": 28,
    "failedCount": 2,
    "failedProducts": [
      {
        "productName": "상품명",
        "brandName": "브랜드명",
        "reason": "중복 상품"
      }
    ],
    "message": "크롤링이 완료되었습니다"
  }
}
```

---

### 2. 특정 브랜드 크롤링

```bash
POST /api/admin/crawling/products/brand
  ?brandName=Nike
  &brandType=SHOES
  &style=sneakers
  &limit=4
```

**파라미터:**
- `brandName`: 브랜드명
- `brandType`: `CLOTHING` | `SHOES` (기본: CLOTHING)
- `style`: 스타일 (minimalist, hip_hop 등)
- `limit`: 카테고리당 개수 (기본: 1)
  - 의류: `limit × 3`개
  - 신발: `limit`개

---

### 3. 신발 랭킹 크롤링

무신사 신발 랭킹에서 상품을 크롤링합니다. 브랜드가 없으면 자동 생성됩니다.

```bash
POST /api/admin/crawling/products/shoes?category=sneakers&limit=20
```

**파라미터:**
- `category`: `sneakers` | `boots` | `sandals` | `dress_shoes` | `sports_shoes` | `all_shoes`
- `limit`: 크롤링 개수 (기본: 20)

---

### 4. 시즌 관리

#### 현재 시즌 조회
```bash
GET /api/admin/crawling/season/current
```

#### 시즌 전환 (SS ↔ FW)
```bash
POST /api/admin/crawling/season/transition
```

**시즌 구분:**
- **SS (Spring/Summer)**: 3월 ~ 8월
- **FW (Fall/Winter)**: 9월 ~ 2월

**시즌 전환 시 동작:**
- 이전 시즌 상품 비활성화 (`isActive = false`)
- 현재 시즌 상품 활성화 (`isActive = true`)
- 사계절 상품 (신발/액세서리)은 항상 활성화

---

## 🏗️ 아키텍처

### 동작 흐름

```
1. API 호출
   ↓
2. 브랜드 타입 확인 (CLOTHING | SHOES)
   ↓
3. 크롤링 (트랜잭션 밖)
   - 의류: 상의/아우터/하의 각 limit개
   - 신발: limit개
   ↓
4. 저장 (트랜잭션 내부)
   - 배치 중복 체크 (1번 쿼리)
   - 배치 저장 (1번 쿼리)
   ↓
5. 실패 목록 반환
```

### 성능 최적화

**트랜잭션 분리:**
- 변경 전: 크롤링(5초) + 저장(1초) = 6초 트랜잭션
- 변경 후: 저장(1초)만 트랜잭션 → **83% 단축**

**배치 중복 체크:**
- 변경 전: 100개 상품 = 100번 쿼리
- 변경 후: 100개 상품 = **2번 쿼리** (99% 감소)

---

## 📊 크롤링 대상 브랜드

### 의류 브랜드 (12개)

| 스타일 | 브랜드 |
|--------|--------|
| 힙합 | Outstanding, Draw Fit |
| 미니멀 | Musinsa Standard, Dimitri Black |
| 스트릿 | Trillion, Espionage |
| 고프코어 | The North Face, National Geographic |
| 아메카지 | Frizmworks, Toffee |
| 시티보이 | Thisisneverthat, Suare |

### 신발 브랜드 (10개)

| 신발 타입 | 브랜드 |
|----------|--------|
| 스니커즈 | Nike, Adidas |
| 로퍼 | Gucci, Tod's |
| 부츠 | Dr. Martens, Timberland |
| 구두 | Clarks, Cole Haan |
| 샌들 | Birkenstock, Teva |

---

## ⚙️ 무신사 API 정보

### PLP API (Product Listing Page)

```
GET https://api.musinsa.com/api2/dp/v1/plp/goods
  ?gf=M
  &sortCode=POPULAR
  &category=001
  &brand=musinsastandard
  &page=1
  &size=30
  &caller=FLAGSHIP
```

**주요 파라미터:**
- `gf`: 성별 (M/F)
- `sortCode`: POPULAR, SALE_RATE, LOW_PRICE, HIGH_PRICE, NEW
- `category`: 001(상의), 002(아우터), 003(하의), 103(신발)
- `brand`: 브랜드 코드 (소문자, 공백/특수문자 제거)

**필수 헤더:**
```
Accept: application/json
Origin: https://www.musinsa.com
Referer: https://www.musinsa.com/
```

### 신발 랭킹 API

```
GET https://api.musinsa.com/api2/hm/web/v5/pans/ranking/sections/256
  ?storeCode=sneaker
  &categoryCode=103004
```

**카테고리 코드:**
- `103000`: 전체 신발
- `103004`: 스니커즈
- `103002`: 부츠/워커
- `103003`: 샌들/슬리퍼

---

## ⚠️ 주의사항

### 크롤링 에티켓
- ✅ 요청 간격 2초 유지
- ✅ 적절한 헤더 설정
- ❌ 과도한 요청 금지

### 법적 고려사항
- 개인 학습/연구 목적으로만 사용
- 상업적 사용 시 무신사와 별도 계약 필요

### 기술적 제한
- 브랜드가 DB에 먼저 등록되어 있어야 함
- API 구조 변경 시 크롤링 실패 가능

---

## 🐛 트러블슈팅

**문제: 크롤링이 실패합니다**
- 브랜드 DB 등록 확인
- 무신사 접속 가능 여부 확인
- 로그 확인

**문제: 상품이 저장되지 않습니다**
- 실패 목록(`failedProducts`)에서 사유 확인
- 중복 상품 여부 확인

**문제: 성능이 느립니다**
- `limit` 값 줄이기
- 특정 브랜드만 크롤링

---

## 📚 참고 문서

- [Brand 스타일 정리](./brand-by-style.md)
- [아키텍처 문서](./ARCHITECTURE.md)
- [데이터 흐름](./DATA_FLOW.md)

## 🔗 관련 파일

- `ProductCrawlingController.java`: API 엔드포인트
- `MusinsaCrawlerService.java`: 무신사 크롤링
- `ProductCrawlingService.java`: DB 저장 로직
- `BrandService.java`: 브랜드 관리
- `ProductSeasonService.java`: 시즌 관리
- `sql/V3__insert_brand_data.sql`: 브랜드 초기 데이터
