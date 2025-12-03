package com.ipzy.domain.recommendation.entity;

import com.ipzy.domain.product.entity.Product;
import com.ipzy.global.common.BaseEntity;
import com.ipzy.global.common.enums.ClothingCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommendation_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClothingCategory category;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "price_snapshot", nullable = false)
    private Integer priceSnapshot;

    @Column(name = "product_name_snapshot", nullable = false, length = 255)
    private String productNameSnapshot;

    @Column(name = "image_url_snapshot", length = 500)
    private String imageUrlSnapshot;

    @Builder
    public RecommendationItem(Recommendation recommendation, Product product,
                              ClothingCategory category, Integer displayOrder) {
        this.recommendation = recommendation;
        this.product = product;
        this.category = category;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        // 스냅샷 저장
        if (product != null) {
            this.priceSnapshot = product.getPrice();
            this.productNameSnapshot = product.getName();
            this.imageUrlSnapshot = product.getImageUrl();
        }
    }

    void setRecommendation(Recommendation recommendation) {
        this.recommendation = recommendation;
    }
}
