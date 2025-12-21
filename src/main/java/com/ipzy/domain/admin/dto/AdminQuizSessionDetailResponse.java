package com.ipzy.domain.admin.dto;

import com.ipzy.domain.quiz.entity.QuizAnswer;
import com.ipzy.domain.quiz.entity.QuizSession;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

@Schema(description = "관리자용 퀴즈 세션 상세 정보")
public record AdminQuizSessionDetailResponse(
    @Schema(description = "세션 ID")
    Long id,

    @Schema(description = "사용자 정보")
    UserSummary user,

    @Schema(description = "퀴즈 정보")
    QuizSummary quiz,

    @Schema(description = "완료 여부")
    Boolean completed,

    @Schema(description = "답변 목록")
    List<AnswerSummary> answers,

    @Schema(description = "생성일")
    LocalDateTime createdAt
) {
    @Schema(description = "사용자 요약 정보")
    public record UserSummary(
        Long id,
        String email,
        String name
    ) {
    }

    @Schema(description = "퀴즈 요약 정보")
    public record QuizSummary(
        Long id,
        String title,
        String description
    ) {
    }

    @Schema(description = "답변 정보")
    public record AnswerSummary(
        Long id,
        Long questionId,
        String questionText,
        List<String> selectedOptions,
        LocalDateTime answeredAt
    ) {
        public static AnswerSummary from(QuizAnswer answer) {
            return new AnswerSummary(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getQuestion().getText(),
                answer.getSelectedOptions(),
                answer.getCreatedAt()
            );
        }
    }

    public static AdminQuizSessionDetailResponse from(QuizSession session) {
        UserSummary userSummary = session.getUser() != null
            ? new UserSummary(
                session.getUser().getId(),
                session.getUser().getEmail(),
                session.getUser().getName()
            )
            : new UserSummary(null, null, "익명");

        return new AdminQuizSessionDetailResponse(
            session.getId(),
            userSummary,
            new QuizSummary(
                session.getQuiz().getId(),
                session.getQuiz().getTitle(),
                session.getQuiz().getDescription()
            ),
            session.getCompleted(),
            session.getAnswers().stream()
                .map(AnswerSummary::from)
                .toList(),
            session.getCreatedAt()
        );
    }
}
