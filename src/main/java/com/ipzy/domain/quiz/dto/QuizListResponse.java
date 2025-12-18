package com.ipzy.domain.quiz.dto;

import com.ipzy.domain.quiz.entity.Quiz;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuizListResponse {

    private Long quizId;
    private String title;
    private String description;
    private Integer displayOrder;

    public static QuizListResponse from(Quiz quiz) {
        if (quiz == null) {
            throw new IllegalArgumentException("Quiz cannot be null");
        }
        return new QuizListResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getDisplayOrder()
        );
    }

}
