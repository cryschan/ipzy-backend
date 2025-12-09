package com.ipzy.domain.product.controller;

import com.ipzy.domain.product.dto.CrawlingResponse;
import com.ipzy.domain.product.service.ProductCrawlingService;
import com.ipzy._global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Product Crawling", description = "상품 크롤링 관리 API")
@RestController
@RequestMapping("/api/admin/crawling")
@RequiredArgsConstructor
public class ProductCrawlingController {

    private final ProductCrawlingService productCrawlingService;

    @Operation(
            summary = "전체 브랜드 상품 크롤링",
            description = "등록된 모든 브랜드의 상품을 크롤링하여 DB에 저장합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "크롤링 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "savedCount": 150,
                                        "message": "크롤링이 완료되었습니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "크롤링 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "PROD_004",
                                        "message": "크롤링에 실패했습니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/products/all")
    public ApiResponse<CrawlingResponse> crawlAllProducts() {
        log.info("전체 상품 크롤링 API 호출");

        int savedCount = productCrawlingService.crawlAndSaveAllProducts();
        CrawlingResponse response = CrawlingResponse.of(savedCount, "크롤링이 완료되었습니다");

        return ApiResponse.success(response);
    }

    @Operation(
            summary = "특정 브랜드 상품 크롤링",
            description = "지정한 브랜드의 상품을 크롤링하여 DB에 저장합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "크롤링 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "savedCount": 10,
                                        "brandName": "Musinsa Standard",
                                        "style": "minimalist",
                                        "message": "크롤링이 완료되었습니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "브랜드를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "PROD_003",
                                        "message": "브랜드를 찾을 수 없습니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "크롤링 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "PROD_004",
                                        "message": "크롤링에 실패했습니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/products/brand")
    public ApiResponse<CrawlingResponse> crawlBrandProducts(
            @RequestParam String brandName,
            @RequestParam(defaultValue = "CLOTHING") String brandType,
            @RequestParam String style,
            @RequestParam(defaultValue = "1") int limit
    ) {
        log.info("브랜드 상품 크롤링 API 호출: brandName={}, brandType={}, style={}, limit={}",
                brandName, brandType, style, limit);

        int savedCount = productCrawlingService.crawlAndSaveBrandProducts(brandName, brandType, style, limit);
        CrawlingResponse response = CrawlingResponse.of(savedCount, brandName, style, "크롤링이 완료되었습니다");

        return ApiResponse.success(response);
    }

    @Operation(
            summary = "신발 랭킹 크롤링",
            description = "무신사 신발 랭킹에서 카테고리별 상품을 크롤링하여 DB에 저장합니다. 브랜드가 없으면 자동으로 생성됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "크롤링 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "savedCount": 20,
                                        "message": "신발 랭킹 크롤링이 완료되었습니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/products/shoes")
    public ApiResponse<CrawlingResponse> crawlShoesRanking(
            @RequestParam(defaultValue = "sneakers") String category,
            @RequestParam(defaultValue = "20") int limit
    ) {
        log.info("신발 랭킹 크롤링 API 호출: category={}, limit={}", category, limit);

        int savedCount = productCrawlingService.crawlAndSaveShoesRanking(category, limit);
        CrawlingResponse response = CrawlingResponse.of(savedCount, "신발 랭킹 크롤링이 완료되었습니다");

        return ApiResponse.success(response);
    }
}