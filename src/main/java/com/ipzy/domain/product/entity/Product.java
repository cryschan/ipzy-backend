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

    @Column(nullable = false)
    private Integer price;

    @Column(name = "original_price")
    private Integer originalPrice;

    @Column(name = "discount_percent")
    private Integer discountPercent = 0;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Type(StringArrayType.class)
    @Column(columnDefinition = "text[]")
    private String[] images;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Type(StringArrayType.class)
    @Column(columnDefinition = "text[]")
    private String[] sizes;

    @Type(StringArrayType.class)
    @Column(columnDefinition = "text[]")
    private String[] colors;

    @Type(StringArrayType.class)
    @Column(columnDefinition = "text[]")
    private String[] tags;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "purchase_url", length = 500)
    private String purchaseUrl;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Product(Brand brand, String name, ClothingCategory category, String subCategory,
                   Integer price, Integer originalPrice, Integer discountPercent,
                   String imageUrl, String[] images, String description,
                   String[] sizes, String[] colors, String[] tags,
                   Integer stock, Boolean isActive, String purchaseUrl) {
        this.brand = brand;
        this.name = name;
        this.category = category;
        this.subCategory = subCategory;
        this.price = price;
        this.originalPrice = originalPrice;
        this.discountPercent = discountPercent != null ? discountPercent : 0;
        this.imageUrl = imageUrl;
        this.images = images;
        this.description = description;
        this.sizes = sizes;
        this.colors = colors;
        this.tags = tags;
        this.stock = stock != null ? stock : 0;
        this.isActive = isActive != null ? isActive : true;
        this.purchaseUrl = purchaseUrl;
    }

    public void updateStock(Integer stock) {
        this.stock = stock;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public boolean isInStock() {
        return this.stock > 0;
    }

    public void softDelete() {
        this.isActive = false;
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
