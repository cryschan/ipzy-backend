package com.ipzy.domain.quiz.dto;

import com.ipzy.domain.quiz.entity.QuizQuestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class QuizQuestionResponse {

    private Long questionId;
    private String text;
    private String type;
    private Boolean required;
    private Integer displayOrder;
    private List<QuizOptionResponse> options;

    public static QuizQuestionResponse from(QuizQuestion question) {
        return new QuizQuestionResponse(
                question.getId(),
                question.getText(),
                question.getType().name(),
                question.getRequired(),
                question.getDisplayOrder(),
                question.getOptions().stream()
                        .map(QuizOptionResponse::from)
                        .toList()
        );
    }

}
