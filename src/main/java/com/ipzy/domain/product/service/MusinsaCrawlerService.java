package com.ipzy.domain.product.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipzy.domain.product.dto.CrawledProductDto;
import com.ipzy.domain.product.dto.MusinsaPlpResponse;
import com.ipzy.domain.product.dto.MusinsaRankingLinkDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

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

    private static final String RANKING_API_TEMPLATE = "/api2/hm/web/v5/pans/ranking/sections/199"
            + "?storeCode=musinsa&gf=A&ageBand=AGE_BAND_ALL&period=DAILY"
            + "&eventPeriod=BASIC_REALTIME&categoryCode=%s&page=1&startRank=1&offset=%d";

    private static final String TARGET_SECTION_NAME = "ranking_goods_list";

    // 카테고리 코드 매핑 (PLP API용 - 단순 코드)
    private static final Map<String, String> CATEGORY_CODES_SHORT = Map.of(
            "TOP", "001",         // 상의
            "OUTER", "002",       // 아우터
            "BOTTOM", "003",      // 바지/하의
            "SHOES", "103",       // 신발
            "ACCESSORY", "101"    // 패션소품
    );

    // 카테고리 코드 매핑 (랭킹 API용 - 상세 코드)
    private static final Map<String, String> CATEGORY_CODES = Map.of(
            "TOP", "001000",      // 상의
            "OUTER", "002000",    // 아우터
            "BOTTOM", "003000",   // 바지/하의
            "SHOES", "103000",    // 신발
            "ACCESSORY", "101000" // 패션소품
    );

    private final RestClient musinsaRestClient;
    private final ObjectMapper objectMapper;

    /**
     * 카테고리별 랭킹 상위 상품 크롤링 (간소화 버전 - 랭킹 API만 사용)
     *
     * @param category 카테고리 (TOP, BOTTOM, OUTER, SHOES, ACCESSORY)
     * @param limit 가져올 상품 수
     * @return 크롤링한 상품 리스트 (URL, 랭킹 정보만)
     */
    public List<CrawledProductDto> crawlCategoryProducts(String category, int limit) {
        log.info("무신사 랭킹 크롤링 시작: 카테고리={}, 수량={}", category, limit);

        String categoryCode = CATEGORY_CODES.get(category.toUpperCase());
        if (categoryCode == null) {
            log.error("유효하지 않은 카테고리: {}", category);
            return List.of();
        }

        List<CrawledProductDto> products = new ArrayList<>();

        try {
            // 랭킹 API에서 상위 N개 URL 가져오기
            List<MusinsaRankingLinkDto> rankingLinks = fetchTopLinksByCategory(categoryCode, limit);

            log.info("랭킹에서 {}개 상품 URL 수집 완료", rankingLinks.size());

            // URL을 CrawledProductDto로 변환 (기본 정보만)
            for (MusinsaRankingLinkDto link : rankingLinks) {
                CrawledProductDto product = CrawledProductDto.builder()
                        .brandName("Unknown")  // 브랜드는 나중에 상세 파싱에서
                        .name(extractProductIdFromUrl(link.url()))  // 임시로 상품 ID 사용
                        .category(category)
                        .subCategory("")
                        .price(0)  // 가격은 나중에
                        .originalPrice(0)
                        .discountPercent(0)
                        .thumbnailImageUrl("")
                        .description("무신사 랭킹 " + link.rank() + "위")
                        .colors(List.of())
                        .purchaseUrl(link.url())
                        .build();

                products.add(product);
            }

        } catch (Exception e) {
            log.error("카테고리 {} 크롤링 실패: {}", category, e.getMessage(), e);
        }

        log.info("크롤링 완료: {} 카테고리 {}개 상품 수집", category, products.size());
        return products;
    }

    /**
     * 브랜드명으로 상품 검색 및 크롤링 (PLP API 사용)
     *
     * @param brandName 브랜드명
     * @param style 스타일 (미사용 - 향후 확장용)
     * @param limit 크롤링할 상품 수
     * @return 크롤링한 상품 리스트
     */
    public List<CrawledProductDto> crawlBrandProducts(String brandName, String style, int limit) {
        log.info("브랜드 상품 크롤링 시작 (PLP API): 브랜드={}, 수량={}", brandName, limit);

        // 브랜드 코드 변환 (예: "Musinsa Standard" -> "musinsastandard")
        String brandCode = convertToBrandCode(brandName);

        List<CrawledProductDto> allProducts = new ArrayList<>();

        // 주요 카테고리별로 크롤링
        for (String category : List.of("TOP", "OUTER", "BOTTOM")) {
            try {
                int perCategoryLimit = Math.max(1, limit / 3); // 최소 1개 보장
                List<CrawledProductDto> products = crawlBrandProductsByCategory(brandCode, category, perCategoryLimit);
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

        log.info("브랜드 상품 크롤링 완료: {} ({}개)", brandName, allProducts.size());
        return allProducts;
    }

    /**
     * 특정 브랜드의 특정 카테고리 상품 크롤링 (PLP API 사용)
     *
     * @param brandCode 브랜드 코드
     * @param category 카테고리 (TOP, OUTER, BOTTOM 등)
     * @param limit 상품 수
     * @return 크롤링한 상품 리스트
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

            // 응답을 CrawledProductDto로 변환
            List<CrawledProductDto> products = response.getData().getList().stream()
                    .limit(limit)
                    .map(item -> convertToDto(item, category))
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

            return objectMapper.readValue(responseBody, MusinsaPlpResponse.class);

        } catch (Exception e) {
            log.error("무신사 PLP API 호출 실패", e);
            return null;
        }
    }

    /**
     * MusinsaPlpResponse.ProductItem을 CrawledProductDto로 변환
     */
    private CrawledProductDto convertToDto(MusinsaPlpResponse.ProductItem item, String category) {
        return CrawledProductDto.builder()
                .brandName(item.getBrandName())
                .name(item.getGoodsName())
                .category(category)
                .subCategory("")
                .price(item.getPrice())
                .originalPrice(item.getNormalPrice())
                .discountPercent(item.getSaleRate())
                .thumbnailImageUrl(item.getThumbnail())
                .description(String.format("리뷰: %d개 (평점: %d점)",
                        item.getReviewCount() != null ? item.getReviewCount() : 0,
                        item.getReviewScore() != null ? item.getReviewScore() : 0))
                .colors(List.of())
                .purchaseUrl(item.getGoodsLinkUrl())
                .build();
    }

    /**
     * 브랜드명을 브랜드 코드로 변환
     * 예: "Musinsa Standard" -> "musinsastandard"
     */
    private String convertToBrandCode(String brandName) {
        return brandName.toLowerCase()
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "");
    }

    /**
     * 카테고리별 랭킹에서 상위 N개 상품 URL 가져오기
     *
     * @param categoryCode 무신사 카테고리 코드 (001000=상의, 002000=아우터, 003000=바지, 103000=신발, 101000=패션소품)
     * @param limit 가져올 상품 수
     * @return 랭킹 링크 리스트
     */
    private List<MusinsaRankingLinkDto> fetchTopLinksByCategory(String categoryCode, int limit) {
        String rankingApi = String.format(RANKING_API_TEMPLATE, categoryCode, limit);
        log.debug("무신사 랭킹 API 호출: categoryCode={}, limit={}", categoryCode, limit);

        try {
            String responseBody = musinsaRestClient.get()
                    .uri(rankingApi)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode dataNode = root.path("data");

            if (dataNode.isMissingNode()) {
                log.warn("무신사 랭킹 API가 data 필드를 반환하지 않음");
                return List.of();
            }

            Iterator<JsonNode> amplitudeNodes = dataNode.findValues("amplitude").iterator();
            List<MusinsaRankingLinkDto> links = new ArrayList<>();
            Set<String> seenUrls = new HashSet<>();  // 중복 URL 체크용

            while (amplitudeNodes.hasNext()) {
                JsonNode amplitudeNode = amplitudeNodes.next();
                JsonNode payload = amplitudeNode.path("payload");

                if (!TARGET_SECTION_NAME.equals(payload.path("section_name").asText())) {
                    continue;
                }

                String url = payload.path("url").asText();
                if (!url.contains("/products/")) {
                    continue;
                }

                String rankText = payload.path("index").asText();
                if (url.isBlank() || rankText.isBlank()) {
                    continue;
                }

                // 중복 URL 제거
                if (seenUrls.contains(url)) {
                    continue;
                }

                try {
                    int rank = Integer.parseInt(rankText);
                    links.add(new MusinsaRankingLinkDto(rank, url));
                    seenUrls.add(url);
                } catch (NumberFormatException e) {
                    log.debug("랭킹 파싱 실패: {}", rankText);
                }
            }

            // 랭킹 순으로 정렬 후 상위 N개만 반환
            links.sort(Comparator.comparingInt(MusinsaRankingLinkDto::rank));
            List<MusinsaRankingLinkDto> result = links.size() > limit ? links.subList(0, limit) : links;

            log.debug("랭킹에서 {}개 상품 URL 추출 (요청: {})", result.size(), limit);
            return result;

        } catch (Exception e) {
            log.error("무신사 랭킹 API 파싱 실패", e);
            return List.of();
        }
    }

    /**
     * URL에서 상품 ID 추출
     */
    private String extractProductIdFromUrl(String url) {
        // URL 형식: https://www.musinsa.com/products/1234567
        String[] parts = url.split("/");
        if (parts.length > 0) {
            return "Product_" + parts[parts.length - 1];
        }
        return "Unknown";
    }

    /**
     * 모든 카테고리의 랭킹 상품 크롤링
     */
    public List<CrawledProductDto> crawlAllBrands() {
        log.info("전체 카테고리 랭킹 크롤링 시작");

        List<CrawledProductDto> allProducts = new ArrayList<>();

        // 각 카테고리별로 상위 5개씩 크롤링
        for (String category : CATEGORY_CODES.keySet()) {
            try {
                List<CrawledProductDto> products = crawlCategoryProducts(category, 5);
                allProducts.addAll(products);

                // 크롤링 간격 (카테고리 간 2초 대기)
                Thread.sleep(2000);

            } catch (InterruptedException e) {
                log.error("크롤링 대기 중 인터럽트 발생", e);
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("전체 카테고리 랭킹 크롤링 완료: 총 {}개 상품 수집", allProducts.size());
        return allProducts;
    }
}
