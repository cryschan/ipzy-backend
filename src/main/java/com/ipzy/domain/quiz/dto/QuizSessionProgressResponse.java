package com.ipzy.domain.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class QuizSessionProgressResponse {

    private Long sessionId;
    private int totalQuestions;
    private int answeredCount;
    private boolean completed;
    private List<QuizAnswerProgressResponse> answers;

}
