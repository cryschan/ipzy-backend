package com.ipzy.domain.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class QuizSessionProgressResponse {

    @Schema(description = "퀴즈 세션 ID", example = "12")
    private Long sessionId;

    @Schema(description = "퀴즈 전체 질문 수", example = "4")
    private int totalQuestions;

    @Schema(description = "사용자가 현재까지 답변한 질문 수", example = "2")
    private int answeredCount;

    @Schema(description = "세션 완료 여부", example = "false")
    private boolean completed;

    @Schema(description = "각 질문의 답변 진행 상태 목록")
    private List<QuizAnswerProgressResponse> answers;


}
