package com.ipzy.domain.admin.controller;

import com.ipzy._global.common.ApiResponse;
import com.ipzy.domain.admin.dto.AdminQuizSessionDetailResponse;
import com.ipzy.domain.admin.dto.AdminQuizSessionResponse;
import com.ipzy.domain.admin.dto.AdminQuizSessionSearchRequest;
import com.ipzy.domain.admin.dto.AdminQuizStatsResponse;
import com.ipzy.domain.admin.exception.AdminException;
import com.ipzy.domain.admin.service.AdminAuthService;
import com.ipzy.domain.admin.service.AdminQuizStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Quiz Stats", description = "관리자용 퀴즈 통계 API")
@RestController
@RequestMapping("/api/admin/quiz-stats")
@RequiredArgsConstructor
public class AdminQuizStatsController {

    private final AdminQuizStatsService adminQuizStatsService;
    private final AdminAuthService adminAuthService;

    @Operation(summary = "퀴즈 전체 통계 조회", description = "퀴즈 참여 현황 전체 통계 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "totalSessions": 5000,
                        "completedSessions": 4200,
                        "incompleteSessions": 800,
                        "completionRate": 84.0,
                        "quizStats": [
                          {
                            "quizId": 1,
                            "quizTitle": "스타일 취향 테스트",
                            "totalSessions": 3000,
                            "completedSessions": 2700,
                            "completionRate": 90.0
                          }
                        ]
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "로그인 필요",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_005",
                        "message": "로그인이 필요합니다"
                      }
                    }
                    """)
            )
        )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<AdminQuizStatsResponse>> getQuizStats(HttpSession session) {
        validateAdminSession(session);
        AdminQuizStatsResponse stats = adminQuizStatsService.getQuizStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @Operation(summary = "퀴즈 세션 목록 조회", description = "검색 조건에 따른 퀴즈 세션 목록 페이징 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "content": [
                          {
                            "id": 1,
                            "userId": 1,
                            "userEmail": "user@example.com",
                            "userName": "홍길동",
                            "quizId": 1,
                            "quizTitle": "스타일 취향 테스트",
                            "completed": true,
                            "answerCount": 10,
                            "createdAt": "2024-01-15T10:30:00"
                          }
                        ],
                        "totalElements": 5000,
                        "totalPages": 250
                      }
                    }
                    """)
            )
        )
    })
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<Page<AdminQuizSessionResponse>>> getSessions(
            @ParameterObject @Valid AdminQuizSessionSearchRequest request,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpSession session
    ) {
        validateAdminSession(session);
        Page<AdminQuizSessionResponse> sessions = adminQuizStatsService.findSessions(request, pageable);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @Operation(summary = "퀴즈 세션 상세 조회", description = "특정 세션의 상세 정보 및 답변 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "id": 1,
                        "user": {
                          "id": 1,
                          "email": "user@example.com",
                          "name": "홍길동"
                        },
                        "quiz": {
                          "id": 1,
                          "title": "스타일 취향 테스트",
                          "description": "당신의 패션 스타일을 찾아보세요"
                        },
                        "completed": true,
                        "answers": [
                          {
                            "id": 1,
                            "questionId": 1,
                            "questionText": "선호하는 컬러는?",
                            "selectedOptionId": 2,
                            "selectedOptionText": "블랙/화이트",
                            "answeredAt": "2024-01-15T10:30:00"
                          }
                        ],
                        "createdAt": "2024-01-15T10:30:00"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "세션을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_014",
                        "message": "퀴즈 세션을 찾을 수 없습니다"
                      }
                    }
                    """)
            )
        )
    })
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<AdminQuizSessionDetailResponse>> getSessionDetail(
            @Parameter(description = "세션 ID", required = true, example = "1")
            @PathVariable Long sessionId,
            HttpSession session
    ) {
        validateAdminSession(session);
        AdminQuizSessionDetailResponse sessionDetail = adminQuizStatsService.findSessionDetail(sessionId);
        return ResponseEntity.ok(ApiResponse.success(sessionDetail));
    }

    private Long validateAdminSession(HttpSession session) {
        Long adminId = adminAuthService.getAdminIdFromSession(session);
        if (adminId == null) {
            throw AdminException.sessionRequired();
        }
        return adminId;
    }
}
