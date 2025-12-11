package com.ipzy.domain.product.dto;

import com.ipzy._global.common.enums.ClothingCategory;
import com.ipzy.domain.product.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "상품 응답")
public class ProductResponse {

    @Schema(description = "상품 ID", example = "1")
    private final Long id;

    @Schema(description = "브랜드 ID", example = "1")
    private final Long brandId;

    @Schema(description = "브랜드명", example = "Nike")
    private final String brandName;

    @Schema(description = "상품명", example = "에어포스 1")
    private final String name;

    @Schema(description = "카테고리", example = "SHOES")
    private final ClothingCategory category;

    @Schema(description = "서브 카테고리", example = "스니커즈")
    private final String subCategory;

    @Schema(description = "주요 스타일", example = "sneakers")
    private final String primaryStyle;

    @Schema(description = "가격", example = "129000")
    private final Integer price;

    @Schema(description = "원가", example = "159000")
    private final Integer originalPrice;

    @Schema(description = "할인율", example = "18")
    private final Integer discountPercent;

    @Schema(description = "썸네일 이미지 URL", example = "https://image.musinsa.com/...")
    private final String thumbnailImageUrl;

    @Schema(description = "상품 설명", example = "리뷰: 100개 (평점: 5점)")
    private final String description;

    @Schema(description = "색상 목록", example = "[\"블랙\", \"화이트\"]")
    private final List<String> colors;

    @Schema(description = "시즌 목록", example = "[\"2025_SS\", \"2025_FW\"]")
    private final List<String> seasons;

    @Schema(description = "활성 상태", example = "true")
    private final Boolean isActive;

    @Schema(description = "구매 URL", example = "https://www.musinsa.com/...")
    private final String purchaseUrl;

    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .brandId(product.getBrand().getId())
                .brandName(product.getBrand().getName())
                .name(product.getName())
                .category(product.getCategory())
                .subCategory(product.getSubCategory())
                .primaryStyle(product.getPrimaryStyle())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .discountPercent(product.getDiscountPercent())
                .thumbnailImageUrl(product.getThumbnailImageUrl())
                .description(product.getDescription())
                .colors(product.getColors() != null ? List.of(product.getColors()) : List.of())
                .seasons(product.getSeasons() != null ? List.of(product.getSeasons()) : List.of())
                .isActive(product.getIsActive())
                .purchaseUrl(product.getPurchaseUrl())
                .build();
    }
}
