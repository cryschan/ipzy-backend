package com.ipzy.domain.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandRequest {

    @NotBlank(message = "브랜드명은 필수입니다")
    private String name;

    @NotBlank(message = "주 스타일은 필수입니다")
    private String primaryStyle;

    @NotBlank(message = "브랜드 타입은 필수입니다")
    private String brandType;

    private String logoUrl;

    /**
     * 무신사 브랜드 검증 여부 (선택)
     * true: 무신사에서 브랜드 존재 여부 검증
     * false: 검증 없이 등록
     */
    @Builder.Default
    private boolean validateMusinsa = false;
}