package com.ipzy.domain.product.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 크롤링한 상품 정보를 담는 DTO
 */
@Data
@Builder
public class CrawledProductDto {

    private String brandName;
    private String name;
    private String category;
    private String subCategory;
    private Integer price;
    private Integer originalPrice;
    private Integer discountPercent;
    private String thumbnailImageUrl;
    private String removedBackgroundImageUrl;
    private String review;
    private List<String> colors;
    private String purchaseUrl;
}
