package com.ipzy.domain.quiz.repository;

import com.ipzy.domain.quiz.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    @Query("""
                SELECT DISTINCT q FROM QuizQuestion q
                LEFT JOIN FETCH q.options o
                WHERE q.quiz.id = :quizId
                ORDER BY q.displayOrder ASC, o.displayOrder ASC
            """)
    List<QuizQuestion> findAllByQuizIdWithOptions(Long quizId);

}
