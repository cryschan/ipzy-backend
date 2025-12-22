package com.ipzy.domain.quiz.repository;

import com.ipzy.domain.quiz.entity.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    Optional<QuizAnswer> findBySessionIdAndQuestionId(Long sessionId, Long questionId);

    /**
     * 질문별 옵션 선택 횟수를 집계합니다.
     * selected_options는 jsonb 배열이므로 jsonb_array_elements_text로 펼칩니다.
     *
     * @return [questionId, optionValue, count]
     */
    @Query(value = """
        SELECT qa.question_id, opt.value AS option_value, COUNT(*) AS cnt
        FROM quiz_answers qa
        CROSS JOIN LATERAL jsonb_array_elements_text(qa.selected_options) AS opt(value)
        GROUP BY qa.question_id, opt.value
        ORDER BY qa.question_id, cnt DESC
        """, nativeQuery = true)
    List<Object[]> countOptionSelections();
}
