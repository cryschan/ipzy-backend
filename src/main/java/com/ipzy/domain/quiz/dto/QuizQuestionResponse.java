package com.ipzy.domain.quiz.dto;

import com.ipzy.domain.quiz.entity.QuizQuestion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestionResponse {

    @Schema(description = "질문 ID", example = "1")
    private Long questionId;

    @Schema(description = "질문 내용", example = "어떻게 보이고 싶어요?")
    private String text;

    @Schema(description = "질문 타입(SINGLE 또는 MULTIPLE)", example = "SINGLE")
    private String type;

    @Schema(description = "필수 질문 여부", example = "true")
    private Boolean required;

    @Schema(description = "질문의 표시 순서", example = "2")
    private Integer displayOrder;

    @Schema(description = "질문에 포함된 옵션 목록")
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
