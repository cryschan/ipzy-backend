package com.ipzy.domain.product.entity;

import com.ipzy._global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "brands",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_brand_name_type",
        columnNames = {"name", "brand_type"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "primary_style", length = 50)
    private String primaryStyle;

    @Column(name = "brand_type", nullable = false, length = 20)
    private String brandType;

    @Builder
    public Brand(String name, String logoUrl, String primaryStyle, String brandType) {
        this.name = name;
        this.logoUrl = logoUrl;
        this.primaryStyle = primaryStyle;
        this.brandType = brandType;
    }

    public void update(String name, String logoUrl, String primaryStyle, String brandType) {
        this.name = name;
        this.logoUrl = logoUrl;
        this.primaryStyle = primaryStyle;
        this.brandType = brandType;
    }
}
