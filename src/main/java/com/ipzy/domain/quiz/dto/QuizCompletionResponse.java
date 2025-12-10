package com.ipzy.domain.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class QuizCompletionResponse {

    @Schema(description = "퀴즈 세션 ID", example = "12")
    private Long sessionId;

    @Schema(description = "세션 완료 여부", example = "false")
    private Boolean completed;

    @Schema(description = "세션이 완료된 시각 (completed = true일 때만 값 존재)",
            example = "2025-01-15T14:23:10")
    private LocalDateTime completedAt;


}
