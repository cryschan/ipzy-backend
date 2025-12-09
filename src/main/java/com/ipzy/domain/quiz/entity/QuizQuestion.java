package com.ipzy.domain.quiz.entity;

import com.ipzy._global.common.BaseEntity;
import com.ipzy._global.common.enums.QuizType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false, length = 500)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizType type = QuizType.SINGLE;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(nullable = false)
    private Boolean required = true;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<QuizOption> options = new ArrayList<>();

    @Builder
    public QuizQuestion(Quiz quiz, String text, QuizType type,
                        Integer displayOrder, Boolean required) {
        this.quiz = quiz;
        this.text = text;
        this.type = type != null ? type : QuizType.SINGLE;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.required = required != null ? required : true;
    }

    void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public void addOption(QuizOption option) {
        this.options.add(option);
        option.setQuestion(this);
    }
}
