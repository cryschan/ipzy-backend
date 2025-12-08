package com.ipzy.domain.quiz.entity;

import com.ipzy.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiz_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(nullable = false, length = 200)
    private String text;

    @Column(nullable = false, length = 100)
    private String value;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Builder
    public QuizOption(QuizQuestion question, String text, String value,
                      String imageUrl, Integer displayOrder) {
        this.question = question;
        this.text = text;
        this.value = value;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    void setQuestion(QuizQuestion question) {
        this.question = question;
    }
}
