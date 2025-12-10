package com.ipzy.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 크롤링 실패한 상품 정보
 *
 * @param productName 상품명
 * @param brandName 브랜드명
 * @param reason 실패 사유 (50자 제한)
 */
@Schema(description = "크롤링 실패 상품 정보")
public record FailedProductInfo(
        @Schema(description = "상품명", example = "올마이티 썸머 레저 셋업")
        String productName,

        @Schema(description = "브랜드명", example = "Musinsa Standard")
        String brandName,

        @Schema(description = "실패 사유", example = "가격 정보 없음")
        String reason
) {
    /**
     * 실패 정보 생성 (사유 50자 제한)
     */
    public static FailedProductInfo of(String productName, String brandName, String reason) {
        String truncatedReason = reason != null && reason.length() > 50
                ? reason.substring(0, 47) + "..."
                : reason;

        return new FailedProductInfo(productName, brandName, truncatedReason);
    }
}
