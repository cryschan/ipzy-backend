package com.ipzy.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관리자 대시보드 통계 응답 DTO
 */
@Schema(description = "관리자 대시보드 통계 응답")
public record AdminDashboardResponse(
    @Schema(description = "사용자 통계")
    UserStats userStats,

    @Schema(description = "퀴즈 통계")
    QuizStats quizStats,

    @Schema(description = "추천 통계")
    RecommendationStats recommendationStats
) {

    @Schema(description = "사용자 통계")
    public record UserStats(
        @Schema(description = "전체 사용자 수", example = "1234")
        long totalUsers,

        @Schema(description = "활성 사용자 수", example = "1100")
        long activeUsers,

        @Schema(description = "이번달 신규 가입자 수", example = "56")
        long newUsersThisMonth
    ) {}

    @Schema(description = "퀴즈 통계")
    public record QuizStats(
        @Schema(description = "전체 퀴즈 세션 수", example = "5000")
        long totalSessions,

        @Schema(description = "완료된 세션 수", example = "4200")
        long completedSessions,

        @Schema(description = "완료율 (%)", example = "84.0")
        double completionRate
    ) {}

    @Schema(description = "추천 통계")
    public record RecommendationStats(
        @Schema(description = "전체 추천 수", example = "3500")
        long totalRecommendations,

        @Schema(description = "이번달 추천 수", example = "320")
        long recommendationsThisMonth
    ) {}
}
