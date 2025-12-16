package com.ipzy.domain.product.entity;

import com.ipzy._global.common.BaseEntity;
import com.ipzy._global.common.enums.ClothingCategory;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClothingCategory category;

    @Column(name = "sub_category", length = 50)
    private String subCategory;

    @Column(name = "primary_style", length = 50)
    private String primaryStyle;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "original_price")
    private Integer originalPrice;

    @Column(name = "discount_percent")
    private Integer discountPercent = 0;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "removed_background_image_url", nullable = true, length = 500)
    private String removedBackgroundImageUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String review;

    @Type(StringArrayType.class)
    @Column(columnDefinition = "text[]")
    private String[] colors;

    @Type(StringArrayType.class)
    @Column(columnDefinition = "text[]")
    private String[] seasons;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "purchase_url", length = 500)
    private String purchaseUrl;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Product(Brand brand, String name, ClothingCategory category, String subCategory,
                   String primaryStyle, Integer price, Integer originalPrice, Integer discountPercent,
                   String imageUrl, String removedBackgroundImageUrl,
                   String review, String[] colors, String[] seasons,
                   Boolean isActive, String purchaseUrl) {
        this.brand = brand;
        this.name = name;
        this.category = category;
        this.subCategory = subCategory;
        this.primaryStyle = primaryStyle;
        this.price = price;
        this.originalPrice = originalPrice;
        this.discountPercent = discountPercent != null ? discountPercent : 0;
        this.imageUrl = imageUrl != null ? imageUrl : "";
        this.removedBackgroundImageUrl = removedBackgroundImageUrl;
        this.review = review;
        this.colors = colors;
        this.seasons = seasons;
        this.isActive = isActive != null ? isActive : true;
        this.purchaseUrl = purchaseUrl;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void softDelete() {
        this.isActive = false;
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
