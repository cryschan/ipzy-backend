package com.ipzy.domain.quiz.repository;

import com.ipzy.domain.quiz.entity.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QuizSessionRepository extends JpaRepository<QuizSession, Long>, JpaSpecificationExecutor<QuizSession> {

    @Query("""
                SELECT DISTINCT s FROM QuizSession s
                JOIN FETCH s.quiz q
                LEFT JOIN FETCH s.answers a
                LEFT JOIN FETCH a.question
                WHERE s.id = :sessionId
            """)
    Optional<QuizSession> findByIdWithAnswers(Long sessionId);

    // 통계용 메서드
    long countByCompleted(Boolean completed);

    List<QuizSession> findByCompletedFalseAndCreatedAtBefore(LocalDateTime cutoff);

    // 퀴즈별 통계 (퀴즈 ID, 총 세션 수, 완료 세션 수)
    @Query("""
        SELECT s.quiz.id, COUNT(s), SUM(CASE WHEN s.completed = true THEN 1 ELSE 0 END)
        FROM QuizSession s
        GROUP BY s.quiz.id
        """)
    List<Object[]> countSessionsByQuiz();

    long countByQuizId(Long quizId);

    long countByQuizIdAndCompleted(Long quizId, Boolean completed);
}
