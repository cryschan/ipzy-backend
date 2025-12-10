package com.ipzy.domain.quiz.controller;

import com.ipzy.domain.auth.dto.CustomUserPrincipal;
import com.ipzy.domain.quiz.dto.QuizQuestionResponse;
import com.ipzy.domain.quiz.dto.QuizSessionStartResponse;
import com.ipzy.domain.quiz.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Quiz", description = "퀴즈 API")
@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    // 퀴즈 세션 시작 API
    @PostMapping("/{quizId}/sessions")
    @Operation(
            summary = "퀴즈 세션 시작",
            description = "특정 퀴즈의 세션을 시작합니다. 비로그인 사용자도 접근 가능합니다."
    )
    @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = QuizSessionStartResponse.class))
    )
    public QuizSessionStartResponse startQuiz(
            @PathVariable Long quizId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getUserId() : null;
        return quizService.startQuiz(quizId, userId);
    }

    // 특정 퀴즈의 전체 질문 조회 API
    @GetMapping("/{quizId}/questions")
    @Operation(
            summary = "퀴즈 질문 목록 조회",
            description = """
                    특정 퀴즈의 모든 질문과 옵션을 조회합니다. 비로그인 사용자도 접근 가능합니다.
                    
                    **에러 코드:**
                    | 코드 | HTTP | 설명 |
                    |------|------|------|
                    | QUIZ_001 | 404 | 퀴즈를 찾을 수 없습니다 |
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "질문 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = QuizQuestionResponse.class))
    )
    public List<QuizQuestionResponse> getQuestions(
            @PathVariable Long quizId
    ) {
        return quizService.getQuestions(quizId);
    }

}
