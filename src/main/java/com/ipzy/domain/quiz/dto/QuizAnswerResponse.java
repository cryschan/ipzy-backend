package com.ipzy.domain.quiz.dto;

import com.ipzy.domain.quiz.entity.QuizAnswer;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizAnswerResponse {

    private Long questionId;
    private List<String> selectedOptions;

    public static QuizAnswerResponse from(QuizAnswer answer) {
        if (answer == null) {
            return null;
        }
        if (answer.getQuestion() == null) {
            throw new IllegalArgumentException("답변에 해당하는 질문이 없습니다");
        }
        return QuizAnswerResponse.builder()
                .questionId(answer.getQuestion().getId())
                .selectedOptions(answer.getSelectedOptions())
                .build();
    }
}
