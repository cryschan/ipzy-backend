package com.ipzy.domain.quiz.dto;

import com.ipzy.domain.quiz.entity.QuizAnswer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizAnswerResponse {

    @Schema(description = "질문 ID", example = "1")
    private Long questionId;

    @Schema(description = "선택한 옵션 value 배열 (단일 선택 시 1개)", example = "[\"clean\", \"basic\"]")
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
