package com.ipzy.domain.admin.dto;

import com.ipzy._global.common.enums.ClothingCategory;
import com.ipzy.domain.product.entity.Brand;
import com.ipzy.domain.product.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Schema(description = "관리자용 상품 상세 정보")
public record AdminProductDetailResponse(
    @Schema(description = "상품 ID")
    Long id,

    @Schema(description = "브랜드 정보")
    BrandSummary brand,

    @Schema(description = "상품명")
    String name,

    @Schema(description = "카테고리")
    ClothingCategory category,

    @Schema(description = "서브 카테고리")
    String subCategory,

    @Schema(description = "주요 스타일")
    String primaryStyle,

    @Schema(description = "가격")
    Integer price,

    @Schema(description = "원가")
    Integer originalPrice,

    @Schema(description = "할인율")
    Integer discountPercent,

    @Schema(description = "이미지 URL")
    String imageUrl,

    @Schema(description = "누끼 이미지 URL")
    String removedBackgroundImageUrl,

    @Schema(description = "리뷰/설명")
    String review,

    @Schema(description = "색상 목록")
    List<String> colors,

    @Schema(description = "시즌 목록")
    List<String> seasons,

    @Schema(description = "활성화 여부")
    Boolean isActive,

    @Schema(description = "구매 URL")
    String purchaseUrl,

    @Schema(description = "삭제일")
    LocalDateTime deletedAt,

    @Schema(description = "등록일")
    LocalDateTime createdAt
) {
    @Schema(description = "브랜드 요약 정보")
    public record BrandSummary(
        Long id,
        String name,
        String brandType
    ) {
        public static BrandSummary from(Brand brand) {
            if (brand == null) {
                return null;
            }
            return new BrandSummary(
                brand.getId(),
                brand.getName(),
                brand.getBrandType()
            );
        }
    }

    public static AdminProductDetailResponse from(Product product) {
        return new AdminProductDetailResponse(
            product.getId(),
            BrandSummary.from(product.getBrand()),
            product.getName(),
            product.getCategory(),
            product.getSubCategory(),
            product.getPrimaryStyle(),
            product.getPrice(),
            product.getOriginalPrice(),
            product.getDiscountPercent(),
            product.getImageUrl(),
            product.getRemovedBackgroundImageUrl(),
            product.getReview(),
            product.getColors() != null ? Arrays.asList(product.getColors()) : List.of(),
            product.getSeasons() != null ? Arrays.asList(product.getSeasons()) : List.of(),
            product.getIsActive(),
            product.getPurchaseUrl(),
            product.getDeletedAt(),
            product.getCreatedAt()
        );
    }
}
