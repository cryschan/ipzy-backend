package com.ipzy.domain.admin.dto;

import com.ipzy.domain.quiz.entity.QuizSession;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자용 퀴즈 세션 정보")
public record AdminQuizSessionResponse(
    @Schema(description = "세션 ID")
    Long id,

    @Schema(description = "사용자 ID (익명 시 null)")
    Long userId,

    @Schema(description = "사용자 이메일 (익명 시 null)")
    String userEmail,

    @Schema(description = "사용자 이름 (익명 시 '익명')")
    String userName,

    @Schema(description = "퀴즈 ID")
    Long quizId,

    @Schema(description = "퀴즈 제목")
    String quizTitle,

    @Schema(description = "완료 여부")
    Boolean completed,

    @Schema(description = "답변 수")
    Integer answerCount,

    @Schema(description = "생성일")
    LocalDateTime createdAt
) {
    public static AdminQuizSessionResponse from(QuizSession session) {
        return new AdminQuizSessionResponse(
            session.getId(),
            session.getUser() != null ? session.getUser().getId() : null,
            session.getUser() != null ? session.getUser().getEmail() : null,
            session.getUser() != null ? session.getUser().getName() : "익명",
            session.getQuiz().getId(),
            session.getQuiz().getTitle(),
            session.getCompleted(),
            session.getAnswers().size(),
            session.getCreatedAt()
        );
    }
}
