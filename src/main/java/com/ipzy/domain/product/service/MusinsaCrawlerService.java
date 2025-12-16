package com.ipzy.domain.product.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipzy.domain.product.dto.BrandValidationResult;
import com.ipzy.domain.product.dto.CrawledProductDto;
import com.ipzy.domain.product.dto.MusinsaPlpResponse;
import com.ipzy.domain.product.exception.ProductErrorCode;
import com.ipzy.domain.product.exception.ProductException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 무신사 웹사이트 크롤링 서비스
 * 랭킹 API를 사용하여 카테고리별 상위 상품 URL을 수집합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MusinsaCrawlerService {

    // PLP API 엔드포인트
    private static final String PLP_API_TEMPLATE = "/api2/dp/v1/plp/goods"
            + "?gf=%s&sortCode=%s&category=%s&brand=%s&page=%d&size=%d&caller=FLAGSHIP";

    // 카테고리 코드 매핑 (PLP API용)
    private static final Map<String, String> CATEGORY_CODES_SHORT = Map.of(
            "TOP", "001",         // 상의
            "OUTER", "002",       // 아우터
            "BOTTOM", "003",      // 바지/하의
            "SHOES", "103",       // 신발
            "ACCESSORY", "101"    // 패션소품
    );

    // 신발 카테고리 코드 매핑 (신발 랭킹 API용)
    private static final Map<String, String> SHOE_CATEGORY_CODES = Map.of(
            "all_shoes", "103000",      // 전체
            "sneakers", "103004",       // 스니커즈
            "boots", "103002",          // 부츠/워커
            "sandals", "103003",        // 샌들/슬리퍼
            "dress_shoes", "103001",    // 구두
            "sports_shoes", "103005",   // 스포츠화
            "padding_shoes", "103007"   // 패딩/퍼신발
    );

    // 색상 추출 정규식 패턴 (성능 최적화: 한 번만 컴파일)
    private static final Pattern COLOR_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");

    private final RestClient musinsaRestClient;
    private final ObjectMapper objectMapper;
    private final ImageProcessingService imageProcessingService;


    /**
     * 브랜드명으로 상품 검색 및 크롤링 (PLP API 사용) - 의류용
     * 의류는 상의/아우터/하의 세트로 크롤링합니다.
     *
     * @param brandName 브랜드명
     * @param style 스타일 (미사용 - 향후 확장용)
     * @param limit 카테고리당 크롤링할 상품 수 (예: 4 입력 → 상의 4개 + 아우터 4개 + 하의 4개 = 총 12개)
     * @return 크롤링한 상품 리스트 (상의/아우터/하의 각 limit개씩, 총 limit * 3개)
     */
    public List<CrawledProductDto> crawlBrandProducts(String brandName, String style, int limit) {
        log.info("의류 브랜드 상품 크롤링 시작 (PLP API): 브랜드={}, 카테고리당 {}개 (총 {}개 예상)",
                 brandName, limit, limit * 3);

        // 브랜드 코드 변환 (예: "Musinsa Standard" -> "musinsastandard")
        String brandCode = convertToBrandCode(brandName);

        List<CrawledProductDto> allProducts = new ArrayList<>();

        // 주요 의류 카테고리별로 크롤링 (상의/아우터/하의 세트)
        for (String category : List.of("TOP", "OUTER", "BOTTOM")) {
            try {
                List<CrawledProductDto> products = crawlBrandProductsByCategory(brandCode, category, limit);
                allProducts.addAll(products);

                // 크롤링 간격
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                log.error("크롤링 대기 중 인터럽트 발생", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("카테고리 {} 크롤링 실패: {}", category, e.getMessage());
            }
        }

        log.info("의류 브랜드 상품 크롤링 완료: {} ({}개 - 상의/아우터/하의 각 {}개)",
                 brandName, allProducts.size(), limit);

        // 누끼 이미지 배치 처리
        processRemoveBackgroundBatch(allProducts);

        return allProducts;
    }

    /**
     * 신발 브랜드 상품 크롤링 (PLP API 사용) - 신발용
     *
     * @param brandName 브랜드명
     * @param style 스타일 (미사용 - 향후 확장용)
     * @param limit 크롤링할 상품 수
     * @return 크롤링한 상품 리스트
     */
    public List<CrawledProductDto> crawlShoeProducts(String brandName, String style, int limit) {
        log.info("신발 브랜드 상품 크롤링 시작 (PLP API): 브랜드={}, 수량={}", brandName, limit);

        // 브랜드 코드 변환 (예: "Nike" -> "nike")
        String brandCode = convertToBrandCode(brandName);

        // SHOES 카테고리만 크롤링
        List<CrawledProductDto> products = crawlBrandProductsByCategory(brandCode, "SHOES", limit);

        log.info("신발 브랜드 상품 크롤링 완료: {} ({}개)", brandName, products.size());

        // 누끼 이미지 배치 처리
        processRemoveBackgroundBatch(products);

        return products;
    }

    /**
     * 특정 브랜드의 특정 카테고리 상품 크롤링 (PLP API 사용)
     *
     * @param brandCode 브랜드 코드
     * @param category 카테고리 (TOP, OUTER, BOTTOM 등)
     * @param limit 상품 수
     * @return 크롤링한 상품 리스트 (null 제외)
     */
    private List<CrawledProductDto> crawlBrandProductsByCategory(String brandCode, String category, int limit) {
        String categoryCode = CATEGORY_CODES_SHORT.get(category.toUpperCase());
        if (categoryCode == null) {
            log.warn("유효하지 않은 카테고리: {}", category);
            return List.of();
        }

        int pageSize = Math.min(limit, 30); // 최대 30개씩
        int page = 1;

        try {
            // PLP API 호출
            MusinsaPlpResponse response = fetchPlpApi("M", "POPULAR", categoryCode, brandCode, page, pageSize);

            if (response == null || response.getData() == null || response.getData().getList() == null) {
                log.warn("브랜드 {} 카테고리 {} API 응답 없음", brandCode, category);
                return List.of();
            }

            // 응답을 CrawledProductDto로 변환하고 null 필터링
            List<CrawledProductDto> products = response.getData().getList().stream()
                    .limit(limit)
                    .map(item -> convertToDto(item, category))
                    .filter(Objects::nonNull)  // null 제거 (가격 없는 상품 필터링)
                    .toList();

            log.debug("브랜드 {} 카테고리 {}: {}개 상품 수집", brandCode, category, products.size());
            return products;

        } catch (Exception e) {
            log.error("브랜드 {} 카테고리 {} 크롤링 실패: {}", brandCode, category, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 무신사 PLP API 호출
     *
     * @param gender 성별 필터 (M/F)
     * @param sortCode 정렬 기준 (POPULAR, SALE_RATE 등)
     * @param categoryCode 카테고리 코드
     * @param brandCode 브랜드 코드
     * @param page 페이지 번호
     * @param size 페이지당 상품 수
     * @return API 응답
     */
    private MusinsaPlpResponse fetchPlpApi(String gender, String sortCode, String categoryCode,
                                           String brandCode, int page, int size) {
        String plpApi = String.format(PLP_API_TEMPLATE, gender, sortCode, categoryCode, brandCode, page, size);
        log.debug("무신사 PLP API 호출: brand={}, category={}, page={}, size={}", brandCode, categoryCode, page, size);

        try {
            String responseBody = musinsaRestClient.get()
                    .uri(plpApi)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Origin", "https://www.musinsa.com")
                    .header("Referer", "https://www.musinsa.com/")
                    .retrieve()
                    .body(String.class);

            // 디버그: 원본 응답 일부 로깅 (카테고리 정보 확인용)
            if (responseBody != null && responseBody.contains("list")) {
                log.debug("=== 무신사 API 응답 샘플 (카테고리 필드 확인용) ===");
                log.debug(responseBody.length() > 1000
                        ? responseBody.substring(0, 1000) + "..."
                        : responseBody);
            }

            return objectMapper.readValue(responseBody, MusinsaPlpResponse.class);

        } catch (Exception e) {
            log.error("무신사 PLP API 호출 실패", e);
            return null;
        }
    }

    /**
     * MusinsaPlpResponse.ProductItem을 CrawledProductDto로 변환
     *
     * @return 유효한 DTO, 필수 필드가 없으면 null 반환
     */
    private CrawledProductDto convertToDto(MusinsaPlpResponse.ProductItem item, String category) {
        // 필수 필드 검증: brandName, goodsName
        if (item.getBrandName() == null || item.getBrandName().isBlank()) {
            log.warn("브랜드명이 없는 상품 건너뜀: goodsName={}", item.getGoodsName());
            return null;
        }
        if (item.getGoodsName() == null || item.getGoodsName().isBlank()) {
            log.warn("상품명이 없는 상품 건너뜀: brandName={}", item.getBrandName());
            return null;
        }

        // 필수 필드 검증: price가 없거나 0 이하면 null 반환
        Integer price = item.getPrice();
        if (price == null || price <= 0) {
            log.warn("가격 정보가 없는 상품 건너뜀: brandName={}, productName={}",
                    item.getBrandName(), item.getGoodsName());
            return null;
        }

        // 썸네일 이미지 URL 가져오기
        String thumbnailUrl = item.getThumbnail();

        // 디버그: 썸네일이 없는 경우 전체 item 정보 로깅
        if (thumbnailUrl == null || thumbnailUrl.isBlank()) {
            log.debug("썸네일 누락 상품 디버그 - goodsName={}, goodsNo={}, goodsLinkUrl={}, thumbnail={}, brandName={}",
                    item.getGoodsName(), item.getGoodsNo(), item.getGoodsLinkUrl(),
                    item.getThumbnail(), item.getBrandName());
        }

        // 할인율 먼저 가져오기
        Integer saleRate = item.getSaleRate() != null ? item.getSaleRate() : 0;

        // originalPrice 처리
        Integer normalPrice = item.getNormalPrice();
        if (normalPrice == null || normalPrice <= 0) {
            // normalPrice가 없지만 할인율이 있으면 역으로 계산
            if (saleRate > 0) {
                normalPrice = price * 100 / (100 - saleRate);
                log.debug("원가 역계산: price={}, saleRate={}%, calculated normalPrice={}",
                        price, saleRate, normalPrice);
            } else {
                // 할인도 없고 원가도 없으면 price 사용
                normalPrice = price;
            }
        }

        // subCategory: 무신사 API에서 못 가져오므로 상세 페이지에서 추출 시도
        String subCategory = null;

        // PLP API 응답에서 시도 (대부분 null이지만 혹시 모르니 먼저 시도)
        if (item.getCategory3rdName() != null && !item.getCategory3rdName().isBlank()) {
            subCategory = item.getCategory3rdName();
        } else if (item.getCategory2ndName() != null && !item.getCategory2ndName().isBlank()) {
            subCategory = item.getCategory2ndName();
        }

        // PLP API에서 못 가져왔으면 상세 페이지에서 추출
        if (subCategory == null || subCategory.isBlank()) {
            String productUrl = item.getGoodsLinkUrl();
            if (productUrl != null && !productUrl.isBlank()) {
                subCategory = fetchSubCategoryFromDetailPage(productUrl);
                if (subCategory != null && !subCategory.isBlank()) {
                    log.debug("상세 페이지에서 서브카테고리 추출 성공: {} -> {}",
                            item.getGoodsName(), subCategory);
                } else {
                    log.debug("서브카테고리 추출 실패: goodsName={}, url={}",
                            item.getGoodsName(), productUrl);
                    subCategory = ""; // null 대신 빈 문자열
                }
            } else {
                log.debug("상품 URL 없음: goodsName={}", item.getGoodsName());
                subCategory = "";
            }
        }

        // colors: API의 goodsColorList 우선 사용, 없으면 상품명에서 추출
        List<String> colors = (item.getGoodsColorList() != null && !item.getGoodsColorList().isEmpty())
                ? item.getGoodsColorList()
                : extractColorsFromName(item.getGoodsName());

        return CrawledProductDto.builder()
                .brandName(item.getBrandName())
                .name(item.getGoodsName())
                .category(category)
                .subCategory(subCategory)
                .price(price)
                .originalPrice(normalPrice)
                .discountPercent(saleRate)
                .thumbnailImageUrl(thumbnailUrl)  // 이미 검증됨
                .review(String.format("리뷰: %d개 (평점: %d점)",
                        item.getReviewCount() != null ? item.getReviewCount() : 0,
                        item.getReviewScore() != null ? item.getReviewScore() : 0))
                .colors(colors)
                .purchaseUrl(item.getGoodsLinkUrl())
                .build();
    }

    /**
     * 상품명에서 색상 추출
     * 예: "올마이티 썸머 레저 셋업 [블랙]" -> ["블랙"]
     *     "신세틱 스웨이드 웨스턴 셔츠 [라이트 브라운]" -> ["라이트 브라운"]
     */
    private List<String> extractColorsFromName(String productName) {
        if (productName == null) {
            return List.of();
        }

        List<String> colors = new ArrayList<>();
        // 대괄호 안의 내용 추출: [색상] (클래스 레벨 상수 사용)
        var matcher = COLOR_PATTERN.matcher(productName);

        while (matcher.find()) {
            String color = matcher.group(1).trim();
            if (!color.isEmpty()) {
                colors.add(color);
            }
        }

        return colors;
    }


    /**
     * 브랜드명을 브랜드 코드로 변환
     * 예: "Musinsa Standard" -> "musinsastandard"
     *
     * @throws ProductException brandName이 null이거나 빈 문자열인 경우
     */
    private String convertToBrandCode(String brandName) {
        if (brandName == null || brandName.isBlank()) {
            throw new ProductException(ProductErrorCode.BRAND_NAME_REQUIRED, "브랜드명은 필수입니다");
        }
        return brandName.toLowerCase()
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "");
    }



    /**
     * 신발 카테고리별 랭킹 크롤링 (신발 전용 API 사용)
     *
     * @param shoeCategory 신발 카테고리 (sneakers, boots, sandals 등)
     * @param limit 가져올 상품 수
     * @return 크롤링한 상품 리스트
     */
    public List<CrawledProductDto> crawlShoesRanking(String shoeCategory, int limit) {
        log.info("신발 랭킹 크롤링 시작: 카테고리={}, 수량={}", shoeCategory, limit);

        // 신발 카테고리 필수 검증
        if (shoeCategory == null || shoeCategory.isBlank()) {
            throw new ProductException(ProductErrorCode.INVALID_SHOE_CATEGORY,
                    "신발 카테고리는 필수입니다. 사용 가능한 카테고리: " + SHOE_CATEGORY_CODES.keySet());
        }

        // 화이트리스트 검증
        String categoryCode = SHOE_CATEGORY_CODES.get(shoeCategory.toLowerCase().trim());
        if (categoryCode == null) {
            throw new ProductException(ProductErrorCode.INVALID_SHOE_CATEGORY,
                    String.format("유효하지 않은 신발 카테고리입니다: '%s'. 사용 가능한 카테고리: %s",
                            shoeCategory, SHOE_CATEGORY_CODES.keySet()));
        }

        List<CrawledProductDto> products = new ArrayList<>();

        try {
            // 신발 랭킹 API 호출
            String apiUrl = String.format(
                    "/api2/hm/web/v5/pans/ranking/sections/256?storeCode=sneaker&categoryCode=%s&contentsId=",
                    categoryCode
            );

            String jsonResponse = musinsaRestClient.get()
                    .uri(apiUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Referer", "https://www.musinsa.com/main/sneaker/ranking")
                    .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                    .retrieve()
                    .body(String.class);

            if (jsonResponse == null) {
                log.error("신발 랭킹 API 응답 없음");
                return List.of();
            }

            // JSON 파싱하여 상품 정보 추출
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode dataNode = root.path("data");
            JsonNode modules = dataNode.path("modules");

            if (modules.isMissingNode()) {
                log.warn("modules 노드 없음");
                log.debug("API 응답: {}", jsonResponse.substring(0, Math.min(500, jsonResponse.length())));
                return List.of();
            }

            log.debug("modules 배열 크기: {}", modules.size());

            int count = 0;
            for (JsonNode module : modules) {
                if (count >= limit) break;

                String moduleType = module.path("type").asText();  // moduleType이 아니라 type!
                log.debug("type: {}", moduleType);
                if (!"MULTICOLUMN".equals(moduleType)) {
                    continue;
                }

                log.debug("MULTICOLUMN 찾음");

                // items 배열에서 상품 정보 추출
                JsonNode items = module.path("items");
                if (items.isMissingNode()) {
                    log.debug("items 노드 없음");
                    continue;
                }
                if (!items.isArray()) {
                    log.debug("items가 배열이 아님");
                    continue;
                }
                log.debug("items 배열 크기: {}", items.size());

                for (JsonNode item : items) {
                    if (count >= limit) break;

                    JsonNode info = item.path("info");
                    if (info.isMissingNode()) continue;

                    String brandName = info.path("brandName").asText("");
                    String productName = info.path("productName").asText("");

                    // 필수 필드 검증: brandName
                    if (brandName.isBlank()) {
                        log.warn("브랜드명이 없는 상품 건너뜀: productName={}", productName);
                        continue;
                    }

                    // 필수 필드 검증: productName
                    if (productName.isBlank()) {
                        log.warn("상품명이 없는 상품 건너뜀: brandName={}", brandName);
                        continue;
                    }

                    // 필수 필드 검증: price
                    int price = info.path("finalPrice").asInt(0);
                    if (price <= 0) {
                        log.warn("가격 정보가 없는 상품 건너뜀: brandName={}, productName={}", brandName, productName);
                        continue;
                    }

                    Integer discountRate = info.path("discountRatio").asInt(0);

                    Integer normalPrice = info.path("normalPrice").asInt(0);
                    if (normalPrice <= 0) {
                        // normalPrice가 없지만 할인율이 있으면 역으로 계산
                        if (discountRate > 0) {
                            normalPrice = price * 100 / (100 - discountRate);
                            log.debug("신발 원가 역계산: price={}, discountRate={}%, calculated normalPrice={}",
                                    price, discountRate, normalPrice);
                        } else {
                            // 할인도 없고 원가도 없으면 price 사용
                            normalPrice = price;
                        }
                    }
                    String thumbnailUrl = info.path("imageUrl").asText("");
                    String productUrl = info.path("goodsLinkUrl").asText("");
                    String purchaseUrl = productUrl.startsWith("http") ? productUrl : "https://www.musinsa.com" + productUrl;

                    // 색상 추출
                    List<String> colors = extractColorsFromName(productName);

                    CrawledProductDto product = CrawledProductDto.builder()
                            .brandName(brandName)
                            .name(productName)
                            .category("SHOES")
                            .subCategory(shoeCategory)
                            .price(price)
                            .originalPrice(normalPrice)
                            .discountPercent(discountRate)
                            .thumbnailImageUrl(thumbnailUrl)
                            .review("")
                            .colors(colors)
                            .purchaseUrl(purchaseUrl)
                            .build();

                    products.add(product);
                    count++;
                }
            }

            log.info("신발 랭킹 크롤링 완료: {}개 상품 수집", products.size());

            // 누끼 이미지 배치 처리
            processRemoveBackgroundBatch(products);

        } catch (Exception e) {
            log.error("신발 랭킹 크롤링 실패: category={}, error={}", shoeCategory, e.getMessage(), e);
        }

        return products;
    }

    /**
     * 무신사에 브랜드가 존재하는지 검증
     *
     * @param brandName 브랜드명 (예: "Musinsa Standard", "Thisisneverthat")
     * @return 브랜드 검증 결과
     */
    public BrandValidationResult validateBrandExists(String brandName) {
        // brandName null/blank 체크
        if (brandName == null || brandName.isBlank()) {
            throw new ProductException(ProductErrorCode.BRAND_NAME_REQUIRED);
        }

        String brandCode = convertToBrandCode(brandName);
        log.info("무신사 브랜드 검증 시작: brandName={}, brandCode={}", brandName, brandCode);

        // 여러 카테고리를 순회하며 브랜드 존재 확인
        // 순서: 남성 상의 → 여성 상의 → 남성 신발 → 여성 신발 → 남성 액세서리
        String[][] searchCombinations = {
                {"M", "001"},  // 남성 TOP
                {"F", "001"},  // 여성 TOP
                {"M", "103"},  // 남성 SHOES
                {"F", "103"},  // 여성 SHOES
                {"M", "101"}   // 남성 ACCESSORY
        };

        for (String[] combo : searchCombinations) {
            String gender = combo[0];
            String category = combo[1];

            try {
                MusinsaPlpResponse response = fetchPlpApi(gender, "POPULAR", category, brandCode, 1, 1);

                if (response != null && response.getData() != null
                        && response.getData().getList() != null
                        && !response.getData().getList().isEmpty()) {

                    String categoryName = getCategoryName(category);
                    String genderName = "M".equals(gender) ? "남성" : "여성";

                    log.info("무신사 브랜드 검증 성공: brandCode={}, 카테고리={}_{}",
                            brandCode, genderName, categoryName);

                    return BrandValidationResult.success(brandCode);
                }
            } catch (Exception e) {
                log.debug("브랜드 검증 실패 (다음 카테고리 시도): brandCode={}, gender={}, category={}, error={}",
                        brandCode, gender, category, e.getMessage());
                // 계속 다음 카테고리 시도
            }
        }

        // 모든 카테고리에서 찾지 못함
        log.warn("무신사 브랜드 검증 실패: brandCode={}, 모든 카테고리에서 상품을 찾을 수 없습니다", brandCode);
        return BrandValidationResult.failure(brandCode, "상품을 찾을 수 없습니다");
    }

    private String getCategoryName(String categoryCode) {
        return switch (categoryCode) {
            case "001" -> "상의";
            case "002" -> "아우터";
            case "003" -> "하의";
            case "103" -> "신발";
            case "101" -> "액세서리";
            default -> categoryCode;
        };
    }

    /**
     * 상품 상세 페이지 HTML에서 서브카테고리 추출
     * data-category-name 속성에서 3depth > 2depth 우선순위로 추출
     *
     * @param productUrl 상품 상세 페이지 URL (예: https://www.musinsa.com/products/3859411)
     * @return 서브카테고리명, 추출 실패 시 null
     */
    private String fetchSubCategoryFromDetailPage(String productUrl) {
        if (productUrl == null || productUrl.isBlank()) {
            return null;
        }

        try {
            log.debug("상품 상세 페이지에서 카테고리 추출 시도: {}", productUrl);

            // 상세 페이지 HTML 가져오기
            String html = musinsaRestClient.get()
                    .uri(productUrl)
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Referer", "https://www.musinsa.com/")
                    .retrieve()
                    .body(String.class);

            if (html == null || html.isBlank()) {
                log.debug("상세 페이지 HTML 응답 없음: {}", productUrl);
                return null;
            }

            // 정규식으로 data-category-id와 data-category-name 추출
            // 3depth 우선 시도
            String category3rd = extractCategoryByDepth(html, "3depth");
            if (category3rd != null && !category3rd.isBlank()) {
                log.debug("3depth 카테고리 추출 성공: {}", category3rd);
                return category3rd;
            }

            // 3depth 없으면 2depth 시도
            String category2nd = extractCategoryByDepth(html, "2depth");
            if (category2nd != null && !category2nd.isBlank()) {
                log.debug("2depth 카테고리 추출 성공: {}", category2nd);
                return category2nd;
            }

            log.debug("카테고리 추출 실패: {}", productUrl);
            return null;

        } catch (Exception e) {
            log.debug("상세 페이지 카테고리 추출 중 오류: {} - {}", productUrl, e.getMessage());
            return null;
        }
    }

    /**
     * HTML에서 특정 depth의 카테고리명 추출
     * window.__MSS__.product.state.category 객체에서 추출
     *
     * @param html HTML 문자열
     * @param depth 추출할 depth (예: "2depth", "3depth")
     * @return 카테고리명, 없으면 null
     */
    private String extractCategoryByDepth(String html, String depth) {
        try {
            // window.__MSS__.product.state 에서 category 정보 찾기
            String depthFieldName = switch (depth) {
                case "3depth" -> "categoryDepth3Name";
                case "2depth" -> "categoryDepth2Name";
                case "1depth" -> "categoryDepth1Name";
                default -> null;
            };

            if (depthFieldName == null) {
                return null;
            }

            // JSON에서 해당 필드 값 추출
            // "categoryDepth2Name":"반소매 티셔츠" 형태 매칭
            String regex = String.format("\"%s\":\"([^\"]+)\"", depthFieldName);
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
            java.util.regex.Matcher matcher = pattern.matcher(html);

            if (matcher.find()) {
                String categoryName = matcher.group(1);
                // 빈 문자열이 아닌 경우에만 반환
                if (!categoryName.isBlank()) {
                    return categoryName;
                }
            }
        } catch (Exception e) {
            log.debug("카테고리 추출 중 오류: {} - {}", depth, e.getMessage());
        }
        return null;
    }

    /**
     * 크롤링한 상품들의 누끼 이미지를 배치로 처리
     * ImageProcessingService를 사용하여 파이썬 서버에서 누끼 제거 후 DTO에 설정
     *
     * @param products 크롤링한 상품 리스트
     */
    private void processRemoveBackgroundBatch(List<CrawledProductDto> products) {
        if (products == null || products.isEmpty()) {
            log.debug("누끼 처리할 상품이 없습니다");
            return;
        }

        try {
            log.info("누끼 이미지 배치 처리 시작: {}개 상품", products.size());

            // 썸네일 URL만 수집 (null 제외)
            List<String> thumbnailUrls = products.stream()
                    .map(CrawledProductDto::getThumbnailImageUrl)
                    .filter(Objects::nonNull)
                    .filter(url -> !url.isBlank())
                    .distinct() // 중복 제거
                    .toList();

            if (thumbnailUrls.isEmpty()) {
                log.warn("유효한 썸네일 URL이 없습니다");
                return;
            }

            // 파이썬 서버에 배치 요청
            Map<String, String> urlMap = imageProcessingService.removeBackgroundBatch(thumbnailUrls);

            // 결과를 각 DTO에 설정
            int successCount = 0;
            for (CrawledProductDto product : products) {
                String thumbnailUrl = product.getThumbnailImageUrl();
                if (thumbnailUrl != null && urlMap.containsKey(thumbnailUrl)) {
                    product.setRemovedBackgroundImageUrl(urlMap.get(thumbnailUrl));
                    successCount++;
                } else {
                    // 누끼 처리 실패 시 원본 썸네일 사용
                    product.setRemovedBackgroundImageUrl(thumbnailUrl);
                    log.debug("누끼 처리 실패, 원본 사용: {}", product.getName());
                }
            }

            log.info("누끼 이미지 배치 처리 완료: {}개 성공 / {}개 요청", successCount, products.size());

        } catch (Exception e) {
            log.error("누끼 이미지 배치 처리 중 오류 발생: {}", e.getMessage(), e);
            // 실패 시 모든 상품에 원본 썸네일 설정
            products.forEach(p -> p.setRemovedBackgroundImageUrl(p.getThumbnailImageUrl()));
        }
    }
}
