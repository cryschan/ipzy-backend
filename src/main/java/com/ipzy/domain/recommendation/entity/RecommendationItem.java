package com.ipzy.domain.recommendation.entity;

import com.ipzy._global.common.BaseEntity;
import com.ipzy._global.common.enums.ClothingCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "recommendation_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class RecommendationItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClothingCategory category;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    // 스냅샷 필드 (Python 응답 데이터 저장)
    @Column(name = "product_name_snapshot", nullable = false, length = 255)
    private String productNameSnapshot;

    @Column(name = "brand_snapshot", length = 100)
    private String brandSnapshot;

    @Column(name = "price_snapshot", nullable = false)
    private Integer priceSnapshot;

    @Column(name = "image_url_snapshot", length = 500)
    private String imageUrlSnapshot;

    @Column(name = "link_url_snapshot", length = 500)
    private String linkUrlSnapshot;

    @Builder
    public RecommendationItem(Recommendation recommendation, Long productId,
                              ClothingCategory category, Integer displayOrder,
                              String productNameSnapshot, String brandSnapshot,
                              Integer priceSnapshot, String imageUrlSnapshot,
                              String linkUrlSnapshot) {
        this.recommendation = recommendation;
        this.productId = productId;
        this.category = category;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.productNameSnapshot = productNameSnapshot;
        this.brandSnapshot = brandSnapshot;
        this.priceSnapshot = priceSnapshot != null ? priceSnapshot : 0;
        this.imageUrlSnapshot = imageUrlSnapshot;
        this.linkUrlSnapshot = linkUrlSnapshot;
    }

    void setRecommendation(Recommendation recommendation) {
        this.recommendation = recommendation;
    }
}
