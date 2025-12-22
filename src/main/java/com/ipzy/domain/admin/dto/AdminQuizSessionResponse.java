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
        var user = session.getUser();
        var quiz = session.getQuiz();
        var answers = session.getAnswers();

        String userName = "익명";
        if (user != null && user.getName() != null) {
            userName = user.getName();
        }

        return new AdminQuizSessionResponse(
            session.getId(),
            user != null ? user.getId() : null,
            user != null ? user.getEmail() : null,
            userName,
            quiz != null ? quiz.getId() : null,
            quiz != null ? quiz.getTitle() : null,
            session.getCompleted(),
            answers != null ? answers.size() : 0,
            session.getCreatedAt()
        );
    }
}
