package com.ipzy.domain.product.dto;

import com.ipzy.domain.product.entity.Brand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class BrandResponse {

    private Long id;
    private String name;
    private String logoUrl;
    private String primaryStyle;
    private String brandType;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    public static BrandResponse from(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .logoUrl(brand.getLogoUrl())
                .primaryStyle(brand.getPrimaryStyle())
                .brandType(brand.getBrandType())
                .createdAt(brand.getCreatedAt())
                .modifiedAt(brand.getModifiedAt())
                .build();
    }
}