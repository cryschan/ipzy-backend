package com.ipzy.domain.quiz.dto;

import com.ipzy.domain.quiz.entity.QuizSession;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "퀴즈 세션 시작 응답")
public class QuizSessionStartResponse {

    @Schema(description = "세션 ID", example = "1")
    private Long sessionId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "퀴즈 ID", example = "1")
    private Long quizId;

    @Schema(description = "완료된 세션인지", example = "false")
    private Boolean completed;

    @Schema(description = "세션 생성 시간")
    private LocalDateTime createdAt;

    public static QuizSessionStartResponse from(QuizSession session) {
        return new QuizSessionStartResponse(
                session.getId(),
                session.getUser() != null ? session.getUser().getId() : null,
                session.getQuiz().getId(),
                session.getCompleted(),
                session.getCreatedAt()
        );
    }

}
