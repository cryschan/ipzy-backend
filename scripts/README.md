
## Java 크롤링 서비스 사용 (권장)

Java 프로젝트에서 직접 크롤링 및 DB 저장이 가능합니다:

### API 엔드포인트

1. **전체 브랜드 크롤링**
   ```bash
   POST /api/admin/crawling/products/all
   ```

2. **특정 브랜드 크롤링**
   ```bash
   POST /api/admin/crawling/products/brand?brandName=Uniqlo&style=minimalist&limit=5
   ```

3. **스타일별 크롤링**
   ```bash
   POST /api/admin/crawling/products/style?style=minimalist
   ```

### 관련 클래스

- `MusinsaCrawlerService`: Jsoup을 사용한 무신사 크롤링
- `ProductCrawlingService`: 크롤링 데이터를 DB에 저장
- `ProductCrawlingController`: 크롤링 API 제공

---

## Python 스크립트 (Deprecated)

무신사 상품 데이터를 크롤링하고 SQL 파일로 변환하는 스크립트 모음

## 파일 목록

### musinsa_crawler.py
무신사에서 브랜드별 인기 상품을 크롤링하여 SQL 파일 생성

## 사용법

### 1. 기본 실행

```bash
cd scripts
python3 musinsa_crawler.py
```

### 2. 출력 파일

실행 후 다음 파일들이 생성됩니다:

1. **products.json**
   - 크롤링한 상품 데이터 (JSON 형식)
   - 디버깅 및 확인용

2. **../src/main/java/com/ipzy/domain/product/sql/V4__insert_product_data.sql**
   - 데이터베이스에 삽입할 SQL INSERT 문
   - Flyway 마이그레이션으로 실행 가능

## 크롤링 대상 브랜드

총 20개 브랜드 (10개 스타일 × 2개 브랜드)

| 스타일 | 브랜드 |
|--------|--------|
| 힙합 (hip_hop) | Stussy, Supreme |
| 아메카지 (amekaji) | Levi's, Lee |
| 고프코어 (gorpcore) | Patagonia, Arc'teryx |
| 댄디 (dandy) | Paul Smith, Massimo Dutti |
| 미니멀 (minimalist) | Uniqlo, COS |
| 스트릿 (street) | Carhartt WIP, Dickies |
| 테크웨어 (techwear) | Nike ACG, Acronym |
| 비즈니스 캐주얼 (business_casual) | Banana Republic, Giordano |
| 시티보이 (cityboy) | Thisisneverthat, Marni |
| 빈티지 (vintage) | Covernat, Discus |

## 현재 상태

⚠️ **주의**: 현재 스크립트는 **더미 데이터**를 생성합니다.

실제 무신사 크롤링을 위해서는 추가 구현이 필요합니다.

## 실제 크롤링 구현 방법

### 필요 라이브러리

```bash
pip install requests beautifulsoup4 selenium webdriver-manager
```

### 방법 1: BeautifulSoup (정적 페이지)

```python
from bs4 import BeautifulSoup
import requests

def crawl_musinsa_product(brand_name):
    url = f"https://www.musinsa.com/search/musinsa/goods?q={brand_name}&category=001"
    headers = {
        'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36'
    }

    response = requests.get(url, headers=headers)
    soup = BeautifulSoup(response.text, 'html.parser')

    # 상품 리스트 파싱
    products = []
    for item in soup.select('.li_box'):
        product = {
            'name': item.select_one('.list_info a').text.strip(),
            'price': item.select_one('.price').text.strip(),
            'image_url': item.select_one('img')['src'],
            'purchase_url': 'https:' + item.select_one('a')['href']
        }
        products.append(product)

    return products
```

### 방법 2: Selenium (동적 페이지)

```python
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager

def crawl_musinsa_selenium(brand_name):
    # Chrome 드라이버 설정
    service = Service(ChromeDriverManager().install())
    driver = webdriver.Chrome(service=service)

    try:
        url = f"https://www.musinsa.com/search/musinsa/goods?q={brand_name}"
        driver.get(url)

        # 페이지 로딩 대기
        time.sleep(2)

        # 상품 요소 찾기
        products = driver.find_elements(By.CSS_SELECTOR, '.li_box')

        results = []
        for product in products:
            name = product.find_element(By.CSS_SELECTOR, '.list_info a').text
            price = product.find_element(By.CSS_SELECTOR, '.price').text
            results.append({'name': name, 'price': price})

        return results

    finally:
        driver.quit()
```

## 크롤링 주의사항

### 1. 로봇 배제 표준 확인

```bash
curl https://www.musinsa.com/robots.txt
```

### 2. 크롤링 에티켓

- 요청 간 적절한 간격 설정 (1-2초)
  ```python
  time.sleep(1)
  ```
- User-Agent 헤더 설정
- 동시 요청 제한
- 서버 부하 최소화

### 3. 법적 고려사항

- 무신사 이용약관 준수
- 개인 학습/연구 목적으로만 사용
- 상업적 사용 시 별도 계약 필요
- 저작권 존중

## 스크립트 커스터마이징

### 브랜드 추가

`musinsa_crawler.py`의 `BRANDS` 딕셔너리에 브랜드 추가:

```python
BRANDS = {
    'hip_hop': ['Stussy', 'Supreme', 'New Brand'],
    # ...
}
```

### 상품 수 조정

`crawl_all_brands()` 메서드에서 `limit` 파라미터 변경:

```python
products = self.search_brand_products(brand, style, limit=3)  # 브랜드당 3개
```

### 카테고리 변경

상의(TOP) 외 다른 카테고리 크롤링:

```python
params = {
    'category': '002',  # 하의
    # '003': 아우터
    # '004': 신발
}
```

## 트러블슈팅

### 문제: requests 모듈 없음

```bash
pip install requests
```

### 문제: 크롤링 차단

- User-Agent 변경
- 요청 간격 증가
- Selenium 사용 고려

### 문제: 한글 인코딩 에러

```python
with open(output_file, 'w', encoding='utf-8') as f:
    # ...
```

## 향후 개선 사항

- [ ] 실제 무신사 API 활용 (파트너 계약 시)
- [ ] BeautifulSoup/Selenium 구현
- [ ] 멀티스레딩으로 크롤링 속도 향상
- [ ] 에러 처리 및 재시도 로직
- [ ] 로깅 추가
- [ ] 크롤링 결과 검증 로직
- [ ] 이미지 다운로드 및 저장
- [ ] 가격 변동 추적

## 참고 자료

- [무신사 웹사이트](https://www.musinsa.com)
- [BeautifulSoup 문서](https://www.crummy.com/software/BeautifulSoup/bs4/doc/)
- [Selenium 문서](https://selenium-python.readthedocs.io/)
