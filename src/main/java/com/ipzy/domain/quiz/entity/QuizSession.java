package com.ipzy.domain.quiz.entity;

import com.ipzy.domain.user.entity.User;
import com.ipzy._global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false)
    private Boolean completed = false;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAnswer> answers = new ArrayList<>();

    @Builder
    public QuizSession(User user, Quiz quiz) {
        this.user = user;
        this.quiz = quiz;
        this.completed = false;
    }

    public void complete() {
        this.completed = true;
    }

    public void addAnswer(QuizAnswer answer) {
        this.answers.add(answer);
        answer.setSession(this);
    }

    public boolean isAnonymous() {
        return this.user == null;
    }
}
