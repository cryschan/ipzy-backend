package com.ipzy.domain.recommendation.controller;

import com.ipzy._global.common.ApiResponse;
import com.ipzy._global.util.SecurityUtil;
import com.ipzy.domain.recommendation.dto.request.RecommendationRequest;
import com.ipzy.domain.recommendation.dto.response.RecommendationSummaryResponse;
import com.ipzy.domain.recommendation.entity.Recommendation;
import com.ipzy.domain.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 추천 API 컨트롤러
 */
@Tag(name = "Recommendation", description = "AI 코디 추천 API")
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(
            summary = "Python 통신 테스트",
            description = """
                    Python AI 서비스와의 통신을 테스트합니다.

                    **사용 예시:**
                    ```
                    GET /api/recommendations/test?msg=안녕하세요
                    ```

                    **참고:** Python API 준비 전까지 Mock 응답을 반환합니다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": "[Mock] Python 응답: 안녕하세요"
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/test")
    public ApiResponse<String> testConnection(@RequestParam String msg) {
        String result = recommendationService.testConnection(msg);
        return ApiResponse.success(result);
    }

    @Operation(
            summary = "[테스트] Python 요청 데이터 미리보기",
            description = """
                    Python AI 서비스에 전송될 요청 데이터를 미리 확인합니다.

                    **용도:** Python API 개발 시 요청 형식 확인용
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "요청 데이터 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "sessionId": 123,
                                        "answers": [
                                          {
                                            "questionId": 1,
                                            "questionText": "선호하는 스타일은?",
                                            "selectedOptions": ["캐주얼", "스트릿"]
                                          }
                                        ]
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "퀴즈 미완료",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "REC302",
                                        "message": "퀴즈가 완료되지 않았습니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "접근 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "REC401",
                                        "message": "해당 세션에 접근 권한이 없습니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "REC301",
                                        "message": "퀴즈 세션을 찾을 수 없습니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/sessions/{sessionId}/preview-request")
    public ApiResponse<RecommendationRequest> previewRequest(
            @Parameter(description = "완료된 퀴즈 세션 ID") @PathVariable Long sessionId) {

        Long currentUserId = SecurityUtil.getCurrentUserIdOrNull();
        RecommendationRequest request = recommendationService.previewRequest(sessionId, currentUserId);
        return ApiResponse.success(request);
    }

    @Operation(
            summary = "코디 추천 생성",
            description = """
                    완료된 퀴즈 세션 기반으로 AI 코디 추천을 생성합니다.

                    **인증:** 필수 (로그인 필요)

                    **동작 흐름:**
                    1. 퀴즈 세션 완료 여부 검증
                    2. 익명 세션인 경우 현재 사용자에게 연결
                    3. Python AI 서비스에 추천 요청
                    4. 추천 결과 저장 및 반환
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "추천 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "recommendationId": 1,
                                          "displayOrder": 1,
                                          "occasion": "데이트",
                                          "season": "봄",
                                          "style": "캐주얼",
                                          "reason": "밝은 색감의 캐주얼 룩입니다.",
                                          "totalPrice": 237000,
                                          "styleBoardUrl": "https://example.com/style1.jpg",
                                          "items": [
                                            {
                                              "itemId": 1,
                                              "productId": 101,
                                              "category": "TOP",
                                              "displayOrder": 1,
                                              "productName": "오버핏 셔츠",
                                              "brand": "무신사 스탠다드",
                                              "price": 59000,
                                              "imageUrl": "https://example.com/img1.jpg",
                                              "linkUrl": "https://example.com/product1"
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "퀴즈 미완료",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "REC302",
                                        "message": "퀴즈가 완료되지 않았습니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "AUTH_001",
                                        "message": "인증이 필요합니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "접근 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "REC401",
                                        "message": "해당 세션에 접근 권한이 없습니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "REC301",
                                        "message": "퀴즈 세션을 찾을 수 없습니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 추천 존재",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "REC303",
                                        "message": "이미 추천이 생성된 세션입니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "AI 서비스 연결 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "REC101",
                                        "message": "AI 서비스에 연결할 수 없습니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "504",
                    description = "AI 서비스 타임아웃",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "REC102",
                                        "message": "AI 서비스 응답 시간이 초과되었습니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/sessions/{sessionId}/generate")
    public ApiResponse<List<RecommendationSummaryResponse>> generateRecommendation(
            @Parameter(description = "완료된 퀴즈 세션 ID") @PathVariable Long sessionId) {

        Long currentUserId = SecurityUtil.getCurrentUserIdOrThrow();
        List<Recommendation> recommendations = recommendationService.generateRecommendation(sessionId, currentUserId);

        return ApiResponse.success(
                recommendations.stream()
                        .map(RecommendationSummaryResponse::from)
                        .toList()
        );
    }

    @Operation(
            summary = "코디 추천 재생성",
            description = """
                    이미 추천이 생성된 세션에서 새로운 추천을 다시 생성합니다.

                    **인증:** 필수 (로그인 필요)

                    **동작 흐름:**
                    1. 퀴즈 세션 완료 여부 검증
                    2. 세션 소유권 확인
                    3. Python AI 서비스에 추천 요청
                    4. 새로운 추천 결과 저장 및 반환

                    **참고:** 기존 추천과 별개로 새로운 추천이 추가됩니다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "추천 재생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "recommendationId": 2,
                                          "displayOrder": 1,
                                          "occasion": "출근",
                                          "season": "봄",
                                          "style": "미니멀",
                                          "reason": "깔끔한 오피스 룩입니다.",
                                          "totalPrice": 189000,
                                          "items": [...]
                                        }
                                      ]
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "퀴즈 미완료",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "REC302",
                                        "message": "퀴즈가 완료되지 않았습니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "AUTH_001",
                                        "message": "인증이 필요합니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "접근 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "REC401",
                                        "message": "해당 세션에 접근 권한이 없습니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "REC301",
                                        "message": "퀴즈 세션을 찾을 수 없습니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "AI 서비스 연결 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "REC101",
                                        "message": "AI 서비스에 연결할 수 없습니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "504",
                    description = "AI 서비스 타임아웃",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "REC102",
                                        "message": "AI 서비스 응답 시간이 초과되었습니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/sessions/{sessionId}/regenerate")
    public ApiResponse<List<RecommendationSummaryResponse>> regenerateRecommendation(
            @Parameter(description = "완료된 퀴즈 세션 ID") @PathVariable Long sessionId) {

        Long currentUserId = SecurityUtil.getCurrentUserIdOrThrow();
        List<Recommendation> recommendations = recommendationService.regenerateRecommendation(sessionId, currentUserId);

        return ApiResponse.success(
                recommendations.stream()
                        .map(RecommendationSummaryResponse::from)
                        .toList()
        );
    }

    @Operation(
            summary = "내 추천 히스토리 조회",
            description = """
                    현재 로그인한 사용자의 모든 추천 히스토리를 조회합니다.

                    **인증:** 필수 (로그인 필요)

                    **반환:** 최신순 정렬된 추천 목록
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "추천 히스토리 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "recommendationId": 1,
                                          "displayOrder": 1,
                                          "occasion": "데이트",
                                          "season": "봄",
                                          "style": "캐주얼",
                                          "reason": "밝은 색감의 캐주얼 룩입니다.",
                                          "totalPrice": 237000,
                                          "items": [...]
                                        }
                                      ]
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "AUTH_001",
                                        "message": "인증이 필요합니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/me")
    public ApiResponse<List<RecommendationSummaryResponse>> getMyRecommendations() {

        Long currentUserId = SecurityUtil.getCurrentUserIdOrThrow();
        List<Recommendation> recommendations = recommendationService.getRecommendationsByUser(currentUserId);

        return ApiResponse.success(
                recommendations.stream()
                        .map(RecommendationSummaryResponse::from)
                        .toList()
        );
    }

    @Operation(
            summary = "세션별 추천 조회",
            description = """
                    특정 퀴즈 세션에서 생성된 추천 목록을 조회합니다.

                    **인증:** 선택 (비로그인 허용)
                    - 익명 세션: 누구나 조회 가능
                    - 로그인 세션: 소유자만 조회 가능
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "추천 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "recommendationId": 1,
                                          "displayOrder": 1,
                                          "occasion": "데이트",
                                          "season": "봄",
                                          "style": "캐주얼",
                                          "reason": "밝은 색감의 캐주얼 룩입니다.",
                                          "totalPrice": 237000,
                                          "items": [...]
                                        }
                                      ]
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "접근 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "REC401",
                                        "message": "해당 세션에 접근 권한이 없습니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "REC301",
                                        "message": "퀴즈 세션을 찾을 수 없습니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<List<RecommendationSummaryResponse>> getRecommendationsBySession(
            @Parameter(description = "퀴즈 세션 ID") @PathVariable Long sessionId) {

        Long currentUserId = SecurityUtil.getCurrentUserIdOrNull();
        List<Recommendation> recommendations = recommendationService.getRecommendationsBySession(sessionId, currentUserId);

        return ApiResponse.success(
                recommendations.stream()
                        .map(RecommendationSummaryResponse::from)
                        .toList()
        );
    }
}
