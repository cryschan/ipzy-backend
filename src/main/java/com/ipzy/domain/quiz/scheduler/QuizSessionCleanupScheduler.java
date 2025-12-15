package com.ipzy.domain.quiz.scheduler;

import com.ipzy.domain.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 미완료 퀴즈 세션 정리 스케줄러
 * 
 * 일정 시간 이상 진행되지 않은 미완료 세션을 주기적으로 삭제합니다.
 * 설정: app.quiz.session.cleanup.enabled=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.quiz.session.cleanup.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class QuizSessionCleanupScheduler {

    private final QuizService quizService;

    @Value("${app.quiz.session.cleanup.expiration:PT1H}")
    private String expirationDuration;

    /**
     * 만료된 미완료 퀴즈 세션을 정리합니다.
     * 
     * 실행 주기: app.quiz.session.cleanup.cron 설정값 (기본: 매일 오전 4시)
     * 만료 기준: app.quiz.session.cleanup.expiration 설정값 (기본: 1시간)
     */
    @Scheduled(cron = "${app.quiz.session.cleanup.cron:0 0 4 * * *}")
    public void cleanupExpiredQuizSessions() {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("[QuizSessionCleanup] 시작 - 만료 시간: {}", expirationDuration);
            
            Duration expiration;
            try {
                expiration = Duration.parse(expirationDuration);
            } catch (Exception e) {
                log.error("[QuizSessionCleanup] 만료 시간 파싱 실패 - 값: {}, 형식: ISO-8601 Duration (예: PT1H, PT30M)", 
                        expirationDuration, e);
                return;
            }
            
            int deletedCount = quizService.cleanupExpiredSessions(expiration);
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[QuizSessionCleanup] 완료 - 삭제된 세션: {}개, 실행 시간: {}ms", 
                    deletedCount, executionTime);
                    
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[QuizSessionCleanup] 실패 - 실행 시간: {}ms, 에러: {}", 
                    executionTime, e.getMessage(), e);
            // 예외 발생해도 다음 스케줄 실행에는 영향 없도록 처리
        }
    }
}
