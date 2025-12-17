package com.ipzy.domain.admin.controller;

import com.ipzy._global.common.ApiResponse;
import com.ipzy.domain.admin.dto.AdminDashboardResponse;
import com.ipzy.domain.admin.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 대시보드 API
 */
@Tag(name = "Admin Dashboard", description = "관리자 대시보드 통계 API")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "대시보드 통계 조회", description = "사용자, 퀴즈, 추천 통계를 조회합니다")
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
                        "userStats": {
                          "totalUsers": 1234,
                          "activeUsers": 1100,
                          "newUsersThisMonth": 56
                        },
                        "quizStats": {
                          "totalSessions": 5000,
                          "completedSessions": 4200,
                          "completionRate": 84.0
                        },
                        "recommendationStats": {
                          "totalRecommendations": 3500,
                          "recommendationsThisMonth": 320
                        }
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
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "관리자 권한 필요",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_002",
                        "message": "관리자 권한이 없습니다"
                      }
                    }
                    """)
            )
        )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboardStats() {
        AdminDashboardResponse response = adminDashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
