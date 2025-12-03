package com.ipzy.domain.outfit.entity;

import com.ipzy.domain.recommendation.entity.Recommendation;
import com.ipzy.domain.user.entity.User;
import com.ipzy.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "saved_outfits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedOutfit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @Column(length = 500)
    private String note;

    @Builder
    public SavedOutfit(User user, Recommendation recommendation, String note) {
        this.user = user;
        this.recommendation = recommendation;
        this.note = note;
    }

    public void updateNote(String note) {
        this.note = note;
    }
}
