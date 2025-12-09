package com.ipzy.domain.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class QuizAnswerProgressResponse {

    private Long questionId;
    private List<String> selectedOptions;

}
