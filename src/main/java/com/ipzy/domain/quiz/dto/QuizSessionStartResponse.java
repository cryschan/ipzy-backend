package com.ipzy.domain.quiz.dto;

import com.ipzy.domain.quiz.entity.QuizSession;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "퀴즈 세션 시작 응답")
public class QuizSessionStartResponse {

    @Schema(description = "퀴즈 세션 ID", example = "1")
    private Long sessionId;

    @Schema(description = "세션을 생성한 사용자 ID (비회원일 경우 null)", example = "1")
    private Long userId;

    @Schema(description = "세션이 속한 퀴즈 ID", example = "1")
    private Long quizId;

    @Schema(description = "세션 완료 여부", example = "false")
    private Boolean completed;

    @Schema(description = "세션 생성 시각", example = "2025-01-15T10:23:45")
    private LocalDateTime createdAt;

    public static QuizSessionStartResponse from(QuizSession session) {
        if (session == null) {
            throw new IllegalArgumentException("QuizSession cannot be null");
        }
        if (session.getQuiz() == null) {
            throw new IllegalArgumentException("QuizSession must have a quiz");
        }
        return new QuizSessionStartResponse(
                session.getId(),
                session.getUser() != null ? session.getUser().getId() : null,
                session.getQuiz().getId(),
                session.getCompleted(),
                session.getCreatedAt()
        );
    }

}
