package com.ipzy.domain.recommendation.dto.request;

import com.ipzy.domain.quiz.entity.QuizAnswer;

import java.util.List;

/**
 * 퀴즈 답변 DTO - Python FastAPI 요청용
 */
public record QuizAnswerDto(
        Long questionId,
        String questionText,
        List<String> selectedOptions
) {
    public static QuizAnswerDto from(QuizAnswer answer) {
        return new QuizAnswerDto(
                answer.getQuestion().getId(),
                answer.getQuestion().getText(),
                answer.getSelectedOptions()
        );
    }
}