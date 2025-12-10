package com.ipzy.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class BrandValidationResult {

    /**
     * 무신사에서 브랜드가 존재하는지 여부
     */
    private boolean exists;

    /**
     * 브랜드 코드 (무신사에서 사용하는 코드)
     */
    private String brandCode;

    /**
     * 검증 메시지
     */
    private String message;

    /**
     * 검증 중 발견된 상품 수 (존재하는 경우)
     */
    private Integer productCount;

    public static BrandValidationResult success(String brandCode, int productCount) {
        return BrandValidationResult.builder()
                .exists(true)
                .brandCode(brandCode)
                .message(String.format("무신사에서 브랜드를 찾았습니다. (상품 %d개)", productCount))
                .productCount(productCount)
                .build();
    }

    public static BrandValidationResult failure(String brandCode, String reason) {
        return BrandValidationResult.builder()
                .exists(false)
                .brandCode(brandCode)
                .message(String.format("무신사에서 브랜드를 찾을 수 없습니다: %s", reason))
                .productCount(0)
                .build();
    }
}
