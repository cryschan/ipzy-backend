package com.ipzy.domain.quiz.repository;

import com.ipzy.domain.quiz.entity.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    Optional<QuizAnswer> findBySessionIdAndQuestionId(Long sessionId, Long questionId);

}
