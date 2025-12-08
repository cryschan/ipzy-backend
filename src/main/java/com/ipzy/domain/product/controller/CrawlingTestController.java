package com.ipzy.domain.product.controller;

import com.ipzy.domain.product.dto.CrawledProductDto;
import com.ipzy.domain.product.service.MusinsaCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 크롤링 테스트용 컨트롤러
 * 실제 크롤링 결과를 JSON으로 확인
 */
@Slf4j
@RestController
@RequestMapping("/api/test/crawling")
@RequiredArgsConstructor
public class CrawlingTestController {

    private final MusinsaCrawlerService musinsaCrawlerService;

    /**
     * 브랜드 크롤링 테스트
     * GET /api/test/crawling/brand?name=Uniqlo&limit=3
     */
    @GetMapping("/brand")
    public ResponseEntity<List<CrawledProductDto>> testCrawlBrand(
            @RequestParam String name,
            @RequestParam(defaultValue = "3") int limit
    ) {
        log.info("크롤링 테스트 시작: 브랜드={}, limit={}", name, limit);

        List<CrawledProductDto> products = musinsaCrawlerService.crawlBrandProducts(name, "test", limit);

        log.info("크롤링 결과: {}개 상품", products.size());

        return ResponseEntity.ok(products);
    }

    /**
     * HTML 구조 확인용 - 디버깅
     * GET /api/test/crawling/html?name=Uniqlo
     */
    @GetMapping("/html")
    public ResponseEntity<String> testHtmlStructure(@RequestParam String name) {
        log.info("HTML 구조 확인: 브랜드={}", name);

        try {
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect(
                    "https://www.musinsa.com/search/musinsa/goods?q=" + name
            )
            .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
            .timeout(10000)
            .get();

            // 첫 번째 상품 요소만 추출
            org.jsoup.select.Elements products = doc.select("#searchList li.li_box");

            if (products.isEmpty()) {
                return ResponseEntity.ok("❌ #searchList li.li_box 셀렉터로 상품을 찾을 수 없습니다.\n\n전체 HTML:\n" + doc.html());
            }

            String firstProduct = products.first().html();

            return ResponseEntity.ok("✅ 찾은 상품 수: " + products.size() + "\n\n첫 번째 상품 HTML:\n" + firstProduct);

        } catch (Exception e) {
            log.error("HTML 가져오기 실패", e);
            return ResponseEntity.internalServerError().body("에러: " + e.getMessage());
        }
    }
}
