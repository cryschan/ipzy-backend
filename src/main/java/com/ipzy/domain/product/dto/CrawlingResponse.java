package com.ipzy.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "크롤링 결과 응답")
public class CrawlingResponse {

    @Schema(description = "저장 성공 상품 개수", example = "10")
    private final int savedCount;

    @Schema(description = "저장 실패 상품 개수", example = "2")
    private final int failedCount;

    @Schema(description = "브랜드명 (브랜드별 크롤링 시)", example = "Uniqlo")
    private final String brandName;

    @Schema(description = "스타일 (브랜드별 크롤링 시)", example = "minimalist")
    private final String style;

    @Schema(description = "메시지", example = "크롤링이 완료되었습니다")
    private final String message;

    @Schema(description = "실패 상품 목록")
    private final List<FailedProductInfo> failedProducts;

    public static CrawlingResponse of(int savedCount, String message) {
        return CrawlingResponse.builder()
                .savedCount(savedCount)
                .failedCount(0)
                .message(message)
                .failedProducts(List.of())
                .build();
    }

    public static CrawlingResponse of(int savedCount, int failedCount,
                                      List<FailedProductInfo> failedProducts, String message) {
        return CrawlingResponse.builder()
                .savedCount(savedCount)
                .failedCount(failedCount)
                .failedProducts(failedProducts != null ? failedProducts : List.of())
                .message(message)
                .build();
    }

    public static CrawlingResponse of(int savedCount, String brandName, String style, String message) {
        return CrawlingResponse.builder()
                .savedCount(savedCount)
                .failedCount(0)
                .brandName(brandName)
                .style(style)
                .message(message)
                .failedProducts(List.of())
                .build();
    }

    public static CrawlingResponse of(int savedCount, int failedCount, String brandName, String style,
                                      List<FailedProductInfo> failedProducts, String message) {
        return CrawlingResponse.builder()
                .savedCount(savedCount)
                .failedCount(failedCount)
                .brandName(brandName)
                .style(style)
                .failedProducts(failedProducts != null ? failedProducts : List.of())
                .message(message)
                .build();
    }
}