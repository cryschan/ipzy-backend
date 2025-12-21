package com.ipzy.domain.admin.dto;

import com.ipzy._global.common.enums.ClothingCategory;
import com.ipzy.domain.product.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;

@Schema(description = "관리자용 상품 목록 정보")
public record AdminProductResponse(
    @Schema(description = "상품 ID", example = "prod1")
    String id,

    @Schema(description = "상품명", example = "오버핏 옥스포드 셔츠")
    String name,

    @Schema(description = "브랜드명", example = "무신사 스탠다드")
    String brand,

    @Schema(description = "카테고리 (top, bottom, shoes)", example = "top")
    String category,

    @Schema(description = "가격", example = "59000")
    Integer price,

    @Schema(description = "이미지 URL (없으면 null)", example = "https://...")
    String imageUrl,

    @Schema(description = "구매 링크", example = "https://musinsa.com")
    String externalUrl,

    @Schema(description = "판매 가능 여부", example = "true")
    Boolean isAvailable,

    @Schema(description = "등록일 (yyyy-MM-dd)", example = "2025-01-01")
    String createdAt
) {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static AdminProductResponse from(Product product) {
        String categoryValue = mapCategory(product.getCategory());
        String imageUrl = product.getImageUrl();
        if (imageUrl != null && imageUrl.isEmpty()) {
            imageUrl = null;
        }

        return new AdminProductResponse(
            "prod" + product.getId(),
            product.getName(),
            product.getBrand().getName(),
            categoryValue,
            product.getPrice(),
            imageUrl,
            product.getPurchaseUrl(),
            product.getIsActive(),
            product.getCreatedAt() != null ? product.getCreatedAt().format(DATE_FORMAT) : null
        );
    }

    private static String mapCategory(ClothingCategory category) {
        if (category == null) return "top";
        return switch (category) {
            case TOP -> "top";
            case BOTTOM -> "bottom";
            case OUTER -> "top"; // outer → top으로 매핑
            case SHOES -> "shoes";
            case ACCESSORY, UNKNOWN -> "top"; // 기타는 top으로
        };
    }
}
