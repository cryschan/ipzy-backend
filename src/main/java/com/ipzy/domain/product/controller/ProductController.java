package com.ipzy.domain.product.controller;

import com.ipzy._global.common.ApiResponse;
import com.ipzy.domain.product.dto.ProductResponse;
import com.ipzy.domain.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "Product", description = "상품 조회 API")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(
            summary = "전체 상품 조회",
            description = """
                    크롤링된 모든 상품을 조회합니다.

                    **조회 옵션:**
                    - activeOnly=false (기본값): 모든 상품 조회
                    - activeOnly=true: 활성화된 상품만 조회 (현재 시즌에 맞는 상품)

                    **응답 정보:**
                    - 삭제된 상품은 제외됩니다
                    - 브랜드 정보 포함
                    - 가격, 할인율, 시즌 정보 포함
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "id": 1,
                                          "brandId": 1,
                                          "brandName": "Nike",
                                          "name": "에어포스 1 '07",
                                          "category": "SHOES",
                                          "subCategory": "스니커즈",
                                          "primaryStyle": "sneakers",
                                          "price": 129000,
                                          "originalPrice": 159000,
                                          "discountPercent": 18,
                                          "thumbnailImageUrl": "https://image.musinsa.com/...",
                                          "review": "리뷰: 100개 (평점: 5점)",
                                          "colors": ["블랙", "화이트"],
                                          "seasons": ["ALL"],
                                          "isActive": true,
                                          "purchaseUrl": "https://www.musinsa.com/..."
                                        },
                                        {
                                          "id": 2,
                                          "brandId": 2,
                                          "brandName": "Musinsa Standard",
                                          "name": "오버핏 후드 티셔츠",
                                          "category": "TOP",
                                          "subCategory": "후드 티셔츠",
                                          "primaryStyle": "minimalist",
                                          "price": 29900,
                                          "originalPrice": 29900,
                                          "discountPercent": 0,
                                          "thumbnailImageUrl": "https://image.musinsa.com/...",
                                          "review": "리뷰: 50개 (평점: 4점)",
                                          "colors": ["블랙"],
                                          "seasons": ["2025_FW"],
                                          "isActive": true,
                                          "purchaseUrl": "https://www.musinsa.com/..."
                                        }
                                      ]
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "인증 실패 (추후 적용 예정)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "AUTH_001",
                                        "message": "인증이 필요합니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    @GetMapping
    public ApiResponse<List<ProductResponse>> getProducts(
            @Parameter(description = "활성 상품만 조회 여부 (true: 현재 시즌 상품만, false: 모든 상품)", example = "false")
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        log.info("상품 조회 API 호출: activeOnly={}", activeOnly);

        List<ProductResponse> products = activeOnly
                ? productService.getActiveProducts()
                : productService.getAllProducts();

        log.info("상품 조회 완료: {}개", products.size());
        return ApiResponse.success(products);
    }
}
