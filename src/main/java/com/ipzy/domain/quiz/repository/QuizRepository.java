package com.ipzy.domain.quiz.repository;

import com.ipzy.domain.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByIsActiveTrueOrderByDisplayOrderAsc();

    Optional<Quiz> findByIdAndIsActiveTrue(Long id);

}

