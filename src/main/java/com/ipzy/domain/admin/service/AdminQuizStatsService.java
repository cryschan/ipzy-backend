package com.ipzy.domain.admin.service;

import com.ipzy.domain.admin.dto.*;
import com.ipzy.domain.admin.exception.AdminException;
import com.ipzy.domain.admin.specification.AdminQuizSessionSpecification;
import com.ipzy.domain.quiz.entity.QuizOption;
import com.ipzy.domain.quiz.entity.QuizQuestion;
import com.ipzy.domain.quiz.entity.QuizSession;
import com.ipzy.domain.quiz.repository.QuizAnswerRepository;
import com.ipzy.domain.quiz.repository.QuizQuestionRepository;
import com.ipzy.domain.quiz.repository.QuizRepository;
import com.ipzy.domain.quiz.repository.QuizSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminQuizStatsService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAnswerRepository quizAnswerRepository;

    public AdminQuizStatsResponse getQuizStats() {
        // 총 응답 수 (답변 개수)
        long totalResponses = quizAnswerRepository.count();

        // 질문별 옵션 선택 횟수 집계
        List<Object[]> optionCounts = quizAnswerRepository.countOptionSelections();

        // questionId -> (optionValue -> count) 맵 구성
        Map<Long, Map<String, Long>> countMap = new HashMap<>();
        for (Object[] row : optionCounts) {
            Long questionId = ((Number) row[0]).longValue();
            String optionValue = (String) row[1];
            Long count = ((Number) row[2]).longValue();
            countMap.computeIfAbsent(questionId, k -> new HashMap<>())
                .put(optionValue, count);
        }

        // 모든 질문과 옵션 조회 (첫 번째 퀴즈 기준 - 실제로는 모든 퀴즈 통합)
        List<QuizQuestion> allQuestions = quizQuestionRepository.findAll();

        List<AdminQuizStatsResponse.QuestionStat> questionStats = allQuestions.stream()
            .sorted(Comparator.comparing(QuizQuestion::getDisplayOrder))
            .map(q -> {
                Map<String, Long> optionCountMap = countMap.getOrDefault(q.getId(), Map.of());
                long questionTotal = optionCountMap.values().stream().mapToLong(Long::longValue).sum();

                List<AdminQuizStatsResponse.OptionStat> optionStats = q.getOptions().stream()
                    .sorted(Comparator.comparing(QuizOption::getDisplayOrder))
                    .map(opt -> {
                        long cnt = optionCountMap.getOrDefault(opt.getValue(), 0L);
                        int pct = questionTotal > 0 ? (int) Math.round((cnt * 100.0) / questionTotal) : 0;
                        return new AdminQuizStatsResponse.OptionStat(
                            opt.getValue(),
                            opt.getText(),
                            cnt,
                            pct
                        );
                    })
                    .toList();

                return new AdminQuizStatsResponse.QuestionStat(
                    q.getId(),
                    q.getText(),
                    optionStats
                );
            })
            .toList();

        return AdminQuizStatsResponse.of(totalResponses, questionStats);
    }

    public Page<AdminQuizSessionResponse> findSessions(AdminQuizSessionSearchRequest request, Pageable pageable) {
        Specification<QuizSession> spec = Specification
            .where(AdminQuizSessionSpecification.userEmailOrNameContains(request.keyword()))
            .and(AdminQuizSessionSpecification.quizIdEquals(request.quizId()))
            .and(AdminQuizSessionSpecification.completedEquals(request.completed()))
            .and(AdminQuizSessionSpecification.createdAtBetween(request.createdFrom(), request.createdTo()));

        return quizSessionRepository.findAll(spec, pageable)
            .map(AdminQuizSessionResponse::from);
    }

    public AdminQuizSessionDetailResponse findSessionDetail(Long sessionId) {
        QuizSession session = quizSessionRepository.findByIdWithAnswers(sessionId)
            .orElseThrow(AdminException::quizSessionNotFound);
        return AdminQuizSessionDetailResponse.from(session);
    }
}
