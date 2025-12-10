package com.ipzy.domain.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class QuizAnswerProgressResponse {

    @Schema(description = "질문 ID", example = "1")
    private Long questionId;

    @Schema(description = "선택한 옵션 value 배열 (단일 선택 시 1개)", example = "[\"clean\", \"basic\"]")
    private List<String> selectedOptions;

}
