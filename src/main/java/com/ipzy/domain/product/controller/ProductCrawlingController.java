package com.ipzy.domain.product.controller;

import com.ipzy.domain.product.entity.Brand;
import com.ipzy.domain.product.repository.BrandRepository;
import com.ipzy.domain.product.service.ProductCrawlingService;
import com.ipzy.domain.product.service.ProductSeasonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 상품 크롤링 관리 API
 * 관리자가 필요할 때 무신사 상품을 크롤링하여 DB에 저장
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/crawling")
@RequiredArgsConstructor
public class ProductCrawlingController {

    private final ProductCrawlingService productCrawlingService;
    private final ProductSeasonService productSeasonService;
    private final BrandRepository brandRepository;

    /**
     * 전체 브랜드 상품 크롤링 및 저장
     *
     * GET /api/admin/crawling/products/all
     */
    @PostMapping("/products/all")
    public ResponseEntity<Map<String, Object>> crawlAllProducts() {
        log.info("전체 상품 크롤링 API 호출");

        try {
            int savedCount = productCrawlingService.crawlAndSaveAllProducts();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "크롤링 완료");
            response.put("savedCount", savedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("크롤링 실패", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "크롤링 실패: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 특정 브랜드 상품 크롤링
     *
     * POST /api/admin/crawling/products/brand?brandName=Uniqlo&style=minimalist&limit=5
     */
    @PostMapping("/products/brand")
    public ResponseEntity<Map<String, Object>> crawlBrandProducts(
            @RequestParam String brandName,
            @RequestParam String style,
            @RequestParam(defaultValue = "1") int limit
    ) {
        log.info("브랜드 상품 크롤링 API 호출: brandName={}, style={}, limit={}", brandName, style, limit);

        try {
            int savedCount = productCrawlingService.crawlAndSaveBrandProducts(brandName, style, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "크롤링 완료");
            response.put("brandName", brandName);
            response.put("savedCount", savedCount);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("브랜드 없음: {}", brandName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("크롤링 실패", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "크롤링 실패: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 특정 스타일의 모든 브랜드 상품 크롤링
     *
     * POST /api/admin/crawling/products/style?style=minimalist
     */
    @PostMapping("/products/style")
    public ResponseEntity<Map<String, Object>> crawlByStyle(@RequestParam String style) {
        log.info("스타일별 상품 크롤링 API 호출: style={}", style);

        try {
            int savedCount = productCrawlingService.crawlAndSaveByStyle(style);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "크롤링 완료");
            response.put("style", style);
            response.put("savedCount", savedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("크롤링 실패", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "크롤링 실패: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 시즌 전환 (이전 시즌 상품 비활성화, 현재 시즌 상품 활성화)
     *
     * POST /api/admin/crawling/season/transition
     */
    @PostMapping("/season/transition")
    public ResponseEntity<Map<String, Object>> seasonTransition() {
        log.info("시즌 전환 API 호출");

        try {
            String currentSeason = productSeasonService.getCurrentSeason();
            productSeasonService.seasonTransition();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "시즌 전환 완료");
            response.put("currentSeason", currentSeason);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("시즌 전환 실패", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "시즌 전환 실패: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 현재 시즌 조회
     *
     * GET /api/admin/crawling/season/current
     */
    @GetMapping("/season/current")
    public ResponseEntity<Map<String, Object>> getCurrentSeason() {
        String currentSeason = productSeasonService.getCurrentSeason();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("currentSeason", currentSeason);

        return ResponseEntity.ok(response);
    }

    /**
     * 크롤링 상태 확인 (헬스체크)
     *
     * GET /api/admin/crawling/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "크롤링 서비스 정상 작동 중");

        return ResponseEntity.ok(response);
    }

    /**
     * 테스트용 브랜드 생성 (개발/테스트 전용)
     *
     * POST /api/admin/crawling/brands/create?name=musinsastandard&primaryStyle=minimalist
     */
    @PostMapping("/brands/create")
    public ResponseEntity<Map<String, Object>> createBrand(
            @RequestParam String name,
            @RequestParam(defaultValue = "minimalist") String primaryStyle
    ) {
        log.info("브랜드 생성 API 호출: name={}, primaryStyle={}", name, primaryStyle);

        try {
            // 이미 존재하는지 확인
            if (brandRepository.findByName(name).isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "이미 존재하는 브랜드: " + name);
                return ResponseEntity.badRequest().body(response);
            }

            // 브랜드 생성
            Brand brand = Brand.builder()
                    .name(name)
                    .primaryStyle(primaryStyle)
                    .brandType("CLOTHING")
                    .build();

            brandRepository.save(brand);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "브랜드 생성 완료");
            response.put("brandId", brand.getId());
            response.put("brandName", brand.getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("브랜드 생성 실패", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "브랜드 생성 실패: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }
}
