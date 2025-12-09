package com.ipzy.domain.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class QuizCompletionResponse {

    private Long sessionId;
    private boolean completed;
    private LocalDateTime completedAt;

}
