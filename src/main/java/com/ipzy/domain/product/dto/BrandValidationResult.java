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

    public static BrandValidationResult success(String brandCode) {
        return BrandValidationResult.builder()
                .exists(true)
                .brandCode(brandCode)
                .message("무신사에서 브랜드를 찾았습니다")
                .build();
    }

    public static BrandValidationResult failure(String brandCode, String reason) {
        return BrandValidationResult.builder()
                .exists(false)
                .brandCode(brandCode)
                .message(String.format("무신사에서 브랜드를 찾을 수 없습니다: %s", reason))
                .build();
    }
}
