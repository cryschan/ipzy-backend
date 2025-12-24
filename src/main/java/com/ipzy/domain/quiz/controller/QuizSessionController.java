package com.ipzy.domain.quiz.controller;

import com.ipzy._global.common.ApiResponse;
import com.ipzy._global.util.SecurityUtil;
import com.ipzy.domain.quiz.dto.QuizAnswerRequest;
import com.ipzy.domain.quiz.dto.QuizAnswerResponse;
import com.ipzy.domain.quiz.dto.QuizCompletionResponse;
import com.ipzy.domain.quiz.dto.QuizCompletionWithRecommendationsResponse;
import com.ipzy.domain.quiz.service.QuizService;
import com.ipzy.domain.recommendation.entity.Recommendation;
import com.ipzy.domain.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Quiz Session", description = "퀴즈 세션 API")
@RestController
@RequestMapping("/api/quiz-sessions")
@RequiredArgsConstructor
public class QuizSessionController {

    private final QuizService quizService;
    private final RecommendationService recommendationService;

    @PostMapping("/{sessionId}/complete")
    @Operation(
            summary = "세션 완료 처리",
            description = """
                    퀴즈 세션을 완료 처리합니다. 비로그인 사용자도 접근 가능합니다.
                    
                    **자동 추천 생성 옵션:**
                    - `autoGenerate=true` 파라미터를 추가하면 세션 완료 후 자동으로 추천을 생성합니다.
                    - 로그인 사용자만 자동 생성 가능합니다 (비로그인 사용자는 `autoGenerate` 파라미터를 무시).
                    - 추천 생성이 완료된 후 응답이 반환됩니다 (동기 처리, 약 2~10초 소요).
                    - 응답에 추천 목록이 포함됩니다 (`recommendations` 필드).
                    - `autoGenerate=false` 또는 파라미터 없음: 기존처럼 완료만 처리합니다.
                    
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
                    description = "세션 완료 처리 성공 (autoGenerate=false 또는 파라미터 없음)",
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
                    responseCode = "200",
                    description = "세션 완료 + 추천 생성 성공 (autoGenerate=true)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "sessionId": 12,
                                        "completed": true,
                                        "completedAt": "2025-01-15T14:23:10",
                                        "recommendations": [
                                          {
                                            "displayOrder": 1,
                                            "occasion": "데이트",
                                            "season": "봄",
                                            "style": "캐주얼",
                                            "reason": "밝은 색감의 캐주얼 룩입니다.",
                                            "status": "completed",
                                            "jobId": "rec-1",
                                            "createdAt": "2025-01-15T14:23:15",
                                            "completedAt": "2025-01-15T14:23:15",
                                            "result": {
                                              "success": true,
                                              "message": "Recommendation loaded successfully",
                                              "composite_image_url": "https://example.com/composite.png",
                                              "image_width": 1200,
                                              "image_height": 1600,
                                              "total_price": 237000,
                                              "items": [...]
                                            },
                                            "error": null
                                          }
                                        ]
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
    public ApiResponse<?> complete(
            @PathVariable("sessionId") Long sessionId,
            @Parameter(description = "자동 추천 생성 여부 (로그인 사용자만 가능)", example = "true")
            HttpServletRequest request
    ) {
        // autoGenerate 파라미터를 직접 읽어서 처리 (없으면 false)
        String autoGenerateParam = request.getParameter("autoGenerate");
        boolean shouldAutoGenerate = "true".equalsIgnoreCase(autoGenerateParam);
        
        // 세션 완료 처리
        QuizCompletionResponse completion = quizService.completeSession(sessionId);
        
        // 자동 추천 생성 옵션이 활성화된 경우
        if (shouldAutoGenerate) {
            // 로그인 사용자만 자동 생성 가능
            Long currentUserId = SecurityUtil.getCurrentUserIdOrThrow();
            
            // 동기로 추천 생성 (완료 후 추천까지 완료된 상태로 응답)
            List<Recommendation> recommendations = 
                    recommendationService.generateRecommendation(sessionId, currentUserId);
            
            // 완료 + 추천 응답 반환
            return ApiResponse.success(
                    QuizCompletionWithRecommendationsResponse.of(completion, recommendations)
            );
        }
        
        // autoGenerate=false인 경우 기존 응답 반환
        return ApiResponse.success(completion);
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
            @PathVariable("sessionId") Long sessionId,
            @Valid @RequestBody QuizAnswerRequest request
    ) {
        return ApiResponse.success(quizService.saveOrUpdateAnswer(sessionId, request));
    }

}
