package com.ipzy.domain.quiz.repository;

import com.ipzy.domain.quiz.entity.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

    @Query("""
                SELECT DISTINCT s FROM QuizSession s
                JOIN FETCH s.quiz q
                LEFT JOIN FETCH s.answers a
                LEFT JOIN FETCH a.question
                WHERE s.id = :sessionId
            """)
    Optional<QuizSession> findByIdWithAnswers(Long sessionId);

    List<QuizSession> findByCompletedFalseAndCreatedAtBefore(LocalDateTime cutoff);
}
