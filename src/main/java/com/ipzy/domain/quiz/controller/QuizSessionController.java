package com.ipzy.domain.quiz.controller;

import com.ipzy._global.common.ApiResponse;
import com.ipzy.domain.quiz.dto.QuizAnswerRequest;
import com.ipzy.domain.quiz.dto.QuizAnswerResponse;
import com.ipzy.domain.quiz.dto.QuizCompletionResponse;
import com.ipzy.domain.quiz.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Quiz Session", description = "퀴즈 세션 API")
@RestController
@RequestMapping("/api/quiz-sessions")
@RequiredArgsConstructor
public class QuizSessionController {

    private final QuizService quizService;

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
                    | QUIZ_002 | 400 | 퀴즈가 완료되지 않았습니다 |
                    | QUIZ_003 | 404 | 퀴즈 세션을 찾을 수 없습니다 |
                    | QUIZ_004 | 400 | 이미 완료된 퀴즈 세션입니다 |
                    | QUIZ_008 | 400 | 필수 질문에 답변하지 않았습니다 |
                    | QUIZ_013 | 400 | 유효하지 않은 퀴즈 응답입니다 |
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "세션 완료 처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "sessionId": 12,
                                        "completed": true,
                                        "completedAt": "2025-01-15T14:23:10"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "검증 실패 (QUIZ_002, QUIZ_004, QUIZ_008, QUIZ_013)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션을 찾을 수 없음 (QUIZ_003)"
            )
    })
    public ApiResponse<QuizCompletionResponse> complete(
            @PathVariable Long sessionId
    ) {
        return ApiResponse.success(quizService.completeSession(sessionId));
    }

    @PostMapping("/{sessionId}/answers")
    @Operation(
            summary = "답변 저장/수정",
            description = """
                    퀴즈 세션의 답변을 저장하거나 수정합니다. 비로그인 사용자도 접근 가능합니다.
                    
                    **동작:**
                    - 기존 답변이 있으면 업데이트, 없으면 새로 생성
                    - 질문 타입별 검증 (SINGLE: 1개, MULTIPLE: 1개 이상)
                    - 선택한 옵션이 해당 질문의 유효한 옵션인지 확인
                    
                    **에러 코드:**
                    | 코드 | HTTP | 설명 |
                    |------|------|------|
                    | QUIZ_001 | 404 | 퀴즈를 찾을 수 없습니다 |
                    | QUIZ_003 | 404 | 퀴즈 세션을 찾을 수 없습니다 |
                    | QUIZ_004 | 400 | 이미 완료된 퀴즈 세션입니다 |
                    | QUIZ_006 | 404 | 퀴즈 질문을 찾을 수 없습니다 |
                    | QUIZ_007 | 400 | 해당 질문이 세션의 퀴즈에 속하지 않습니다 |
                    | QUIZ_009 | 400 | 단일 선택 질문에는 1개의 옵션만 선택할 수 있습니다 |
                    | QUIZ_012 | 400 | 유효하지 않은 옵션입니다 |
                    | QUIZ_013 | 400 | 유효하지 않은 퀴즈 응답입니다 |
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "답변 저장/수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "questionId": 1,
                                        "selectedOptions": ["clean"]
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "검증 실패 (QUIZ_004, QUIZ_007, QUIZ_009, QUIZ_012, QUIZ_013)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션, 퀴즈 또는 질문을 찾을 수 없음 (QUIZ_001, QUIZ_003, QUIZ_006)"
            )
    })
    public ApiResponse<QuizAnswerResponse> saveAnswer(
            @PathVariable Long sessionId,
            @Valid @RequestBody QuizAnswerRequest request
    ) {
        return ApiResponse.success(quizService.saveOrUpdateAnswer(sessionId, request));
    }

}
