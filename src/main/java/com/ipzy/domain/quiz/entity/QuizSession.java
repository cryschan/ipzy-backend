package com.ipzy.domain.quiz.entity;

import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.quiz.exception.QuizErrorCode;
import com.ipzy.domain.quiz.exception.QuizException;
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

    /**
     * 익명 세션에 사용자를 연결합니다.
     * 비로그인 상태에서 퀴즈를 풀고, 로그인 후 추천을 받을 때 사용됩니다.
     *
     * @param user 연결할 사용자
     * @throws QuizException 이미 사용자가 연결된 세션인 경우 (QUIZ_011)
     */
    public void assignUser(User user) {
        if (this.user != null) {
            throw new QuizException(QuizErrorCode.SESSION_ALREADY_ASSIGNED);
        }
        this.user = user;
    }
}
