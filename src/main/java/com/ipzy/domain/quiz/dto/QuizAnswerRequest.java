package com.ipzy.domain.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswerRequest {

    @NotNull(message = "질문 ID는 필수입니다")
    @Schema(description = "질문 ID", example = "1")
    private Long questionId;

    @NotEmpty(message = "선택한 옵션이 없습니다")
    @Schema(description = "선택한 옵션 value 배열 (단일 선택 시 1개)", example = "[\"clean\", \"basic\"]")
    private List<String> selectedOptions;

}
