package com.ipzy.domain.product.controller;

import com.ipzy.domain.product.dto.CrawlingResponse;
import com.ipzy.domain.product.service.ProductCrawlingService;
import com.ipzy._global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Product Crawling", description = "상품 크롤링 관리 API")
@RestController
@RequestMapping("/api/admin/crawling")
@RequiredArgsConstructor
@Validated
public class ProductCrawlingController {

    private final ProductCrawlingService productCrawlingService;

    @Operation(
            summary = "전체 브랜드 상품 크롤링",
            description = """
                    DB에 등록된 모든 브랜드의 상품을 크롤링하여 저장합니다.

                    **의류 브랜드 (CLOTHING):**
                    - 상의/아우터/하의를 각 limit개씩 크롤링
                    - 총 limit × 3개 크롤링됩니다

                    **신발 브랜드 (SHOES):**
                    - 신발 카테고리에서 limit개 크롤링

                    예: limit=3일 때 (기본값)
                    - 의류 브랜드: 9개 (상의 3 + 아우터 3 + 하의 3)
                    - 신발 브랜드: 3개

                    예: limit=5일 때
                    - 의류 브랜드: 15개 (상의 5 + 아우터 5 + 하의 5)
                    - 신발 브랜드: 5개
                    """
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
    public ApiResponse<CrawlingResponse> crawlAllProducts(
            @RequestParam(defaultValue = "3") @Positive(message = "limit은 양수여야 합니다") int limit
    ) {
        log.info("전체 상품 크롤링 API 호출: 브랜드당 {}개", limit);

        var result = productCrawlingService.crawlAndSaveAllProducts(limit);
        CrawlingResponse response = CrawlingResponse.of(
                result.savedCount(),
                result.failedProducts().size(),
                result.failedProducts(),
                "크롤링이 완료되었습니다"
        );

        return ApiResponse.success(response);
    }

    @Operation(
            summary = "특정 브랜드 상품 크롤링",
            description = """
                    지정한 브랜드의 상품을 크롤링하여 DB에 저장합니다.

                    **의류 브랜드 (CLOTHING):**
                    - 상의/아우터/하의를 각 limit개씩 크롤링합니다
                    - 총 limit × 3개 크롤링됩니다
                    - 예: limit=4 → 상의 4 + 아우터 4 + 하의 4 = 총 12개

                    **신발 브랜드 (SHOES):**
                    - 신발 카테고리에서 limit개 크롤링합니다
                    - 예: limit=4 → 신발 4개
                    """
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
            @RequestParam @NotBlank(message = "브랜드명은 필수입니다") String brandName,
            @RequestParam(defaultValue = "CLOTHING") @NotBlank(message = "브랜드 타입은 필수입니다") String brandType,
            @RequestParam @NotBlank(message = "스타일은 필수입니다") String style,
            @RequestParam(defaultValue = "1") @Positive(message = "limit은 양수여야 합니다") int limit
    ) {
        log.info("브랜드 상품 크롤링 API 호출: brandName={}, brandType={}, style={}, limit={}",
                brandName, brandType, style, limit);

        var result = productCrawlingService.crawlAndSaveBrandProducts(brandName, brandType, style, limit);
        CrawlingResponse response = CrawlingResponse.of(
                result.savedCount(),
                result.failedProducts().size(),
                brandName,
                style,
                result.failedProducts(),
                "크롤링이 완료되었습니다"
        );

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
            @RequestParam(defaultValue = "sneakers") @NotBlank(message = "카테고리는 필수입니다") String category,
            @RequestParam(defaultValue = "20") @Positive(message = "limit은 양수여야 합니다") int limit
    ) {
        log.info("신발 랭킹 크롤링 API 호출: category={}, limit={}", category, limit);

        var result = productCrawlingService.crawlAndSaveShoesRanking(category, limit);
        CrawlingResponse response = CrawlingResponse.of(
                result.savedCount(),
                result.failedProducts().size(),
                result.failedProducts(),
                "신발 랭킹 크롤링이 완료되었습니다"
        );

        return ApiResponse.success(response);
    }
}