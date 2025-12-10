package com.ipzy.domain.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class QuizQuestionResponse {

    private Long questionId;
    private String text;
    private String type;
    private Boolean required;
    private Integer displayOrder;
    private List<QuizOptionResponse> options;

}
