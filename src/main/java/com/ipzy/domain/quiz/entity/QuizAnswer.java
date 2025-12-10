package com.ipzy.domain.quiz.entity;

import com.ipzy._global.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private QuizSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Type(JsonType.class)
    @Column(name = "selected_options", columnDefinition = "jsonb", nullable = false)
    private List<String> selectedOptions = new ArrayList<>();

    @Builder
    public QuizAnswer(QuizSession session, QuizQuestion question, List<String> selectedOptions) {
        this.session = session;
        this.question = question;
        this.selectedOptions = selectedOptions != null ? selectedOptions : new ArrayList<>();
    }

    void setSession(QuizSession session) {
        this.session = session;
    }

    /**
     * 선택한 옵션을 업데이트합니다.
     * 
     * @param selectedOptions 새로운 선택 옵션 value 목록
     */
    public void updateSelectedOptions(List<String> selectedOptions) {
        if (selectedOptions == null) {
            this.selectedOptions = new ArrayList<>();
        } else {
            this.selectedOptions = new ArrayList<>(selectedOptions);
        }
    }
}
