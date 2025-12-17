package com.ipzy.domain.admin.service;

import com.ipzy._global.common.enums.UserStatus;
import com.ipzy.domain.admin.dto.AdminDashboardResponse;
import com.ipzy.domain.admin.dto.AdminDashboardResponse.QuizStats;
import com.ipzy.domain.admin.dto.AdminDashboardResponse.RecommendationStats;
import com.ipzy.domain.admin.dto.AdminDashboardResponse.UserStats;
import com.ipzy.domain.quiz.repository.QuizSessionRepository;
import com.ipzy.domain.recommendation.repository.RecommendationRepository;
import com.ipzy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 관리자 대시보드 통계 서비스
 */
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final RecommendationRepository recommendationRepository;

    /**
     * 대시보드 통계 조회
     */
    public AdminDashboardResponse getDashboardStats() {
        log.info("대시보드 통계 조회 시작");

        UserStats userStats = getUserStats();
        QuizStats quizStats = getQuizStats();
        RecommendationStats recommendationStats = getRecommendationStats();

        log.info("대시보드 통계 조회 완료: users={}, quizSessions={}, recommendations={}",
                userStats.totalUsers(), quizStats.totalSessions(), recommendationStats.totalRecommendations());

        return new AdminDashboardResponse(userStats, quizStats, recommendationStats);
    }

    /**
     * 사용자 통계 조회
     */
    private UserStats getUserStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long newUsersThisMonth = userRepository.countByCreatedAtAfter(getFirstDayOfMonth());

        return new UserStats(totalUsers, activeUsers, newUsersThisMonth);
    }

    /**
     * 퀴즈 통계 조회
     */
    private QuizStats getQuizStats() {
        long totalSessions = quizSessionRepository.count();
        long completedSessions = quizSessionRepository.countByCompleted(true);

        double completionRate = totalSessions > 0
                ? Math.round((double) completedSessions / totalSessions * 1000) / 10.0
                : 0.0;

        return new QuizStats(totalSessions, completedSessions, completionRate);
    }

    /**
     * 추천 통계 조회
     */
    private RecommendationStats getRecommendationStats() {
        long totalRecommendations = recommendationRepository.count();
        long recommendationsThisMonth = recommendationRepository.countByCreatedAtAfter(getFirstDayOfMonth());

        return new RecommendationStats(totalRecommendations, recommendationsThisMonth);
    }

    /**
     * 이번달 1일 00:00:00 반환
     */
    private LocalDateTime getFirstDayOfMonth() {
        return LocalDateTime.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }
}
