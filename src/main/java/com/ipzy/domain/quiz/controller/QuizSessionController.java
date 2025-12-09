package com.ipzy.domain.quiz.controller;

import com.ipzy.domain.quiz.dto.QuizCompletionResponse;
import com.ipzy.domain.quiz.dto.QuizQuestionResponse;
import com.ipzy.domain.quiz.dto.QuizSessionProgressResponse;
import com.ipzy.domain.quiz.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Quiz Session", description = "퀴즈 세션 API")
@RestController
@RequestMapping("/api/quiz-sessions")
@RequiredArgsConstructor
public class QuizSessionController {

    private final QuizService quizService;

    @GetMapping("/{sessionId}/progress")
    @Operation(
            summary = "세션 진행 상태 조회",
            description = """
                    퀴즈 세션의 진행 상태를 조회합니다. 비로그인 사용자도 접근 가능합니다.
                    
                    **응답 정보:**
                    - `sessionId`: 세션 ID
                    - `totalQuestions`: 전체 질문 수
                    - `answeredCount`: 답변한 질문 수
                    - `completed`: 완료 여부
                    - `answers`: 답변 목록 (질문 ID와 선택한 옵션)
                    
                    **에러 코드:**
                    | 코드 | HTTP | 설명 |
                    |------|------|------|
                    | QUIZ_003 | 404 | 퀴즈 세션을 찾을 수 없습니다 |
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "진행 상태 조회 성공",
            content = @Content(schema = @Schema(implementation = QuizSessionProgressResponse.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "세션을 찾을 수 없음 (QUIZ_003)"
    )
    public QuizSessionProgressResponse getProgress(
            @PathVariable Long sessionId
    ) {
        return quizService.getProgress(sessionId);
    }

    @PostMapping("/{sessionId}/complete")
    @Operation(
            summary = "세션 완료 처리",
            description = """
                    퀴즈 세션을 완료 처리합니다. 비로그인 사용자도 접근 가능합니다.
                    
                    **검증 사항:**
                    - 필수 질문에 모두 답변했는지 확인
                    - 선택한 옵션이 유효한 옵션인지 확인
                    - 질문 타입별 답변 개수 검증 (SINGLE: 1개, MULTIPLE: 1개 이상)
                    - 중복 답변 체크
                    
                    **에러 코드:**
                    | 코드 | HTTP | 설명 |
                    |------|------|------|
                    | QUIZ_001 | 404 | 퀴즈를 찾을 수 없습니다 |
                    | QUIZ_002 | 400 | 유효하지 않은 퀴즈 응답입니다 |
                    | QUIZ_003 | 404 | 퀴즈 세션을 찾을 수 없습니다 |
                    | QUIZ_004 | 400 | 이미 완료된 퀴즈 세션입니다 |
                    | QUIZ_005 | 400 | 필수 질문에 답변하지 않았습니다 |
                    | QUIZ_006 | 400 | 퀴즈가 완료되지 않았습니다 |
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "세션 완료 처리 성공",
            content = @Content(schema = @Schema(implementation = QuizCompletionResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "검증 실패 (QUIZ_002, QUIZ_004, QUIZ_005, QUIZ_006)"
    )
    @ApiResponse(
            responseCode = "404",
            description = "세션을 찾을 수 없음 (QUIZ_003)"
    )
    public QuizCompletionResponse complete(
            @PathVariable Long sessionId
    ) {
        return quizService.completeSession(sessionId);
    }

    @GetMapping("/{sessionId}/questions/{order}")
    @Operation(
            summary = "세션별 단일 질문 조회",
            description = """
                    특정 세션의 특정 순서(displayOrder)의 질문을 조회합니다. 비로그인 사용자도 접근 가능합니다.
                    
                    **파라미터:**
                    - `sessionId`: 세션 ID
                    - `order`: 질문 순서 (displayOrder)
                    
                    **에러 코드:**
                    | 코드 | HTTP | 설명 |
                    |------|------|------|
                    | QUIZ_003 | 404 | 퀴즈 세션을 찾을 수 없습니다 |
                    | QUIZ_004 | 400 | 이미 완료된 퀴즈 세션입니다 |
                    | QUIZ_001 | 404 | 퀴즈를 찾을 수 없습니다 |
                    | QUIZ_002 | 400 | 유효하지 않은 퀴즈 응답입니다 (해당 순서의 질문 없음) |
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "질문 조회 성공",
            content = @Content(schema = @Schema(implementation = QuizQuestionResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "검증 실패 (QUIZ_002, QUIZ_004)"
    )
    @ApiResponse(
            responseCode = "404",
            description = "세션 또는 퀴즈를 찾을 수 없음 (QUIZ_001, QUIZ_003)"
    )
    public QuizQuestionResponse getSessionQuestion(
            @PathVariable Long sessionId,
            @PathVariable Integer order
    ) {
        return quizService.getQuestionByOrder(sessionId, order);
    }

}
