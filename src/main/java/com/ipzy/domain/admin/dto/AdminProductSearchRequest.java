package com.ipzy.domain.admin.dto;

import com.ipzy._global.common.enums.ClothingCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "상품 검색 조건")
public record AdminProductSearchRequest(
    @Schema(description = "검색어 (상품명, 브랜드명)", example = "")
    String keyword,

    @Schema(description = "카테고리", example = "TOP")
    ClothingCategory category,

    @Schema(description = "브랜드 ID", example = "1")
    Long brandId,

    @Schema(description = "활성화 여부")
    Boolean isActive,

    @Schema(description = "등록일 시작", example = "2025-01-01")
    LocalDate createdFrom,

    @Schema(description = "등록일 종료", example = "2025-12-31")
    LocalDate createdTo
) {
}
