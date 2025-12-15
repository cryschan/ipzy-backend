package com.ipzy.domain.quiz.controller;

import com.ipzy._global.common.ApiResponse;
import com.ipzy.domain.auth.dto.CustomUserPrincipal;
import com.ipzy.domain.quiz.dto.QuizListResponse;
import com.ipzy.domain.quiz.dto.QuizQuestionResponse;
import com.ipzy.domain.quiz.dto.QuizSessionStartResponse;
import com.ipzy.domain.quiz.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

    // 퀴즈 목록 조회
    @GetMapping
    @Operation(
            summary = "활성화된 퀴즈 목록 조회",
            description = """
                    활성화된 퀴즈 목록을 조회합니다. 비로그인 사용자도 접근 가능합니다.
                    
                    **용도:**
                    - 퀴즈 선택 화면에서 사용할 퀴즈 목록 조회
                    - 퀴즈 메타데이터만 반환 (질문/옵션 정보는 포함하지 않음)
                    
                    **응답 정보:**
                    - `quizId`: 퀴즈 ID
                    - `title`: 퀴즈 제목
                    - `description`: 퀴즈 설명
                    - `displayOrder`: 표시 순서
                    
                    **정렬:**
                    - `displayOrder` 오름차순으로 정렬됩니다.
                    
                    **참고:**
                    - 질문과 옵션 정보가 필요한 경우 `GET /api/quizzes/{quizId}/questions` API를 사용하세요.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "퀴즈 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    public ApiResponse<List<QuizListResponse>> getQuizzes() {
        return ApiResponse.success(quizService.getActiveQuizzes());
    }

    // 퀴즈 세션 시작 API (비회원 가능)
    @PostMapping("/{quizId}/sessions")
    @Operation(
            summary = "퀴즈 세션 시작",
            description = "특정 퀴즈의 세션을 시작합니다. 비로그인 사용자도 접근 가능합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    public ApiResponse<QuizSessionStartResponse> startQuiz(
            @PathVariable Long quizId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getUserId() : null;
        return ApiResponse.success(quizService.startQuiz(quizId, userId));
    }

    // 특정 퀴즈의 전체 질문 조회 API
    @GetMapping("/{quizId}/questions")
    @Operation(
            summary = "퀴즈 질문 목록 조회",
            description = """
                    특정 퀴즈의 모든 질문과 옵션을 조회합니다. 비로그인 사용자도 접근 가능합니다.
                    
                    **용도:**
                    - 특정 퀴즈를 선택한 후, 그 퀴즈의 모든 질문과 옵션을 한 번에 가져올 때 사용
                    - 퀴즈 상세 정보(질문, 옵션)를 포함하여 반환
                    
                    **응답 정보:**
                    - 각 질문의 ID, 텍스트, 타입, 필수 여부, 순서
                    - 각 질문의 모든 옵션 정보 (ID, 텍스트, 값)
                    
                    **에러 코드:**
                    | 코드 | HTTP | 설명 |
                    |------|------|------|
                    | QUIZ_001 | 404 | 퀴즈를 찾을 수 없습니다 |
                    
                    **참고:**
                    - 퀴즈 목록만 필요한 경우 `GET /api/quizzes` API를 사용하세요.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "질문 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    public ApiResponse<List<QuizQuestionResponse>> getQuestions(
            @PathVariable Long quizId
    ) {
        return ApiResponse.success(quizService.getQuestions(quizId));
    }

}
