package com.ipzy.domain.recommendation.entity;

import com.ipzy.domain.quiz.entity.QuizSession;
import com.ipzy.domain.user.entity.User;
import com.ipzy._global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Table(name = "recommendations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Recommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private QuizSession session;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 1;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(length = 1000)
    private String reason;

    @Column(length = 50)
    private String occasion;

    @Column(length = 20)
    private String season;

    @Column(length = 50)
    private String style;

    @Column(name = "style_board_url", length = 500)
    private String styleBoardUrl;

    @Column(name = "image_width")
    private Integer imageWidth;

    @Column(name = "image_height")
    private Integer imageHeight;

    @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecommendationItem> items = new ArrayList<>();

    @Builder
    public Recommendation(User user, QuizSession session, Integer displayOrder,
                          Integer totalPrice, String reason, String occasion,
                          String season, String style, String styleBoardUrl,
                          Integer imageWidth, Integer imageHeight) {
        this.user = user;
        this.session = session;
        this.displayOrder = displayOrder != null ? displayOrder : 1;
        this.totalPrice = totalPrice != null ? totalPrice : 0;
        this.reason = reason;
        this.occasion = occasion;
        this.season = season;
        this.style = style;
        this.styleBoardUrl = styleBoardUrl;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public void addItem(RecommendationItem item) {
        this.items.add(item);
        item.setRecommendation(this);
        recalculateTotalPrice();
    }

    public void recalculateTotalPrice() {
        this.totalPrice = this.items.stream()
                .mapToInt(RecommendationItem::getPriceSnapshot)
                .sum();
    }

    public boolean isAnonymous() {
        return this.user == null;
    }
}
