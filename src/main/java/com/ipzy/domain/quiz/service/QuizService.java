package com.ipzy.domain.quiz.service;

import com.ipzy.domain.quiz.dto.*;
import com.ipzy.domain.quiz.entity.Quiz;
import com.ipzy.domain.quiz.entity.QuizAnswer;
import com.ipzy.domain.quiz.entity.QuizOption;
import com.ipzy.domain.quiz.entity.QuizQuestion;
import com.ipzy.domain.quiz.entity.QuizSession;
import com.ipzy.domain.quiz.exception.QuizErrorCode;
import com.ipzy.domain.quiz.exception.QuizException;
import com.ipzy.domain.quiz.repository.QuizAnswerRepository;
import com.ipzy.domain.quiz.repository.QuizQuestionRepository;
import com.ipzy.domain.quiz.repository.QuizRepository;
import com.ipzy.domain.quiz.repository.QuizSessionRepository;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.repository.UserRepository;
import com.ipzy._global.common.enums.QuizType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final UserRepository userRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAnswerRepository quizAnswerRepository;

    @Transactional(readOnly = true)
    public List<QuizListResponse> getActiveQuizzes() {
        return quizRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .filter(quiz -> quiz != null) // null 필터링 (방어적 코딩)
                .map(QuizListResponse::from)
                .toList();
    }

    @Transactional
    public QuizSessionStartResponse startQuiz(Long quizId, Long userId) {
        // 퀴즈 조회 (활성화된 퀴즈만)
        Quiz quiz = quizRepository.findByIdAndIsActiveTrue(quizId)
                .orElseThrow(() -> new QuizException(QuizErrorCode.QUIZ_NOT_FOUND));

        // 사용자 조회 (로그인한 경우만)
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElse(null);  // 사용자가 없어도 세션 생성은 가능
        }

        // 퀴즈 세션 생성 (로그인 여부에 따라 user_id nullable 처리)
        QuizSession session = QuizSession.builder()
                .quiz(quiz)
                .user(user)  // 로그인한 경우 user 설정, 비로그인인 경우 null
                .build();

        quizSessionRepository.save(session);

        return QuizSessionStartResponse.from(session);
    }

    @Transactional(readOnly = true)
    public List<QuizQuestionResponse> getQuestions(Long quizId) {
        // 퀴즈 존재 여부 확인 (활성화된 퀴즈만)
        quizRepository.findByIdAndIsActiveTrue(quizId)
                .orElseThrow(() -> new QuizException(QuizErrorCode.QUIZ_NOT_FOUND));

        // 질문 목록 조회 (JOIN FETCH로 옵션도 함께 조회)
        List<QuizQuestion> questions =
                quizQuestionRepository.findAllByQuizIdWithOptions(quizId);

        return questions.stream()
                .map(QuizQuestionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizSessionProgressResponse getProgress(Long sessionId) {
        // 세션 조회 (퀴즈, 답변을 JOIN FETCH로 조회)
        QuizSession session = quizSessionRepository
                .findByIdWithAnswers(sessionId)
                .orElseThrow(() -> new QuizException(QuizErrorCode.SESSION_NOT_FOUND));

        // 질문 목록 조회
        List<QuizQuestion> questions = getQuestionsBySession(session);

        // 전체 질문 수
        int totalQuestions = questions.size();
        // 답변한 질문 수
        int answeredCount = session.getAnswers().size();

        // 답변 목록 매핑
        List<QuizAnswerProgressResponse> answerList = session.getAnswers().stream()
                .filter(a -> a != null && a.getQuestion() != null)
                .map(a -> new QuizAnswerProgressResponse(
                        a.getQuestion().getId(),
                        a.getSelectedOptions()
                ))
                .toList();

        return new QuizSessionProgressResponse(
                session.getId(),
                totalQuestions,
                answeredCount,
                session.getCompleted(),
                answerList
        );
    }

    @Transactional
    public QuizCompletionResponse completeSession(Long sessionId) {
        // 세션 조회 (퀴즈, 답변을 JOIN FETCH로 조회)
        QuizSession session = quizSessionRepository
                .findByIdWithAnswers(sessionId)
                .orElseThrow(() -> new QuizException(QuizErrorCode.SESSION_NOT_FOUND));

        // 세션 완료 여부 검증
        validateSessionNotCompleted(session);

        // 질문 목록 조회
        List<QuizQuestion> questions = getQuestionsBySession(session);

        // 답변 검증
        validateAnswers(session, questions);

        // 완료 처리
        session.complete();

        // modifiedAt이 자동으로 업데이트되므로 이를 completedAt으로 사용
        return new QuizCompletionResponse(
                session.getId(),
                true,
                session.getModifiedAt()
        );
    }

    /**
     * 답변 검증
     * - 필수 질문에 답변했는지 확인
     * - 답변 내용이 유효한지 확인 (옵션 값 검증, 타입별 검증)
     * - 중복 답변 체크
     */
    private void validateAnswers(QuizSession session, List<QuizQuestion> questions) {
        // 세션 null 체크
        if (session == null) {
            throw new QuizException(QuizErrorCode.SESSION_NOT_FOUND);
        }

        List<QuizAnswer> answers = session.getAnswers();

        // 답변이 null인 경우 예외 처리
        if (answers == null) {
            throw new QuizException(QuizErrorCode.QUIZ_NOT_COMPLETED,
                    "답변이 없습니다");
        }

        // 질문이 없는 경우 예외 처리
        if (questions == null || questions.isEmpty()) {
            throw new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE,
                    "퀴즈에 질문이 없습니다");
        }

        // 질문 Map 생성 (O(1) 조회를 위해)
        Map<Long, QuizQuestion> questionMap = questions.stream()
                .filter(q -> q != null && q.getId() != null)
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        // 1. 필수 질문 답변 여부 확인 및 중복 답변 체크를 한 번에 처리
        List<Long> answeredQuestionIds = new ArrayList<>();
        for (QuizAnswer answer : answers) {
            // 답변이 null인 경우 예외 처리
            if (answer == null) {
                throw new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE,
                        "유효하지 않은 답변입니다");
            }

            // 질문이 null인 경우 예외 처리
            if (answer.getQuestion() == null) {
                throw new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE,
                        "답변에 해당하는 질문이 없습니다");
            }

            Long questionId = answer.getQuestion().getId();

            // 중복 답변 체크 (같은 질문에 여러 답변이 있는지)
            if (answeredQuestionIds.contains(questionId)) {
                throw new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE,
                        "같은 질문에 중복 답변이 있습니다");
            }
            answeredQuestionIds.add(questionId);

            // 질문 조회 (Map에서 O(1) 조회)
            QuizQuestion question = questionMap.get(questionId);
            if (question == null) {
                throw new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE,
                        "답변에 해당하는 질문을 찾을 수 없습니다: " + questionId);
            }

            List<String> selectedOptions = answer.getSelectedOptions();

            // 옵션 검증 (validateOptions 재사용)
            validateOptions(question, selectedOptions);
        }

        // 2. 필수 질문 답변 여부 확인
        boolean allRequiredAnswered = questions.stream()
                .filter(QuizQuestion::getRequired)
                .allMatch(q -> answeredQuestionIds.contains(q.getId()));

        if (!allRequiredAnswered) {
            throw new QuizException(QuizErrorCode.QUIZ_REQUIRED_NOT_ANSWERED);
        }
    }

    @Transactional
    public QuizAnswerResponse saveOrUpdateAnswer(Long sessionId, QuizAnswerRequest request) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new QuizException(QuizErrorCode.SESSION_NOT_FOUND));

        // 세션 완료 여부 검증
        validateSessionNotCompleted(session);

        QuizQuestion question = quizQuestionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new QuizException(QuizErrorCode.QUIZ_QUESTION_NOT_FOUND));

        // 퀴즈가 null인 경우 예외 처리
        if (session.getQuiz() == null) {
            throw new QuizException(QuizErrorCode.QUIZ_NOT_FOUND);
        }

        // 질문이 세션의 퀴즈에 속해 있는지 검증
        if (question.getQuiz() == null || !question.getQuiz().getId().equals(session.getQuiz().getId())) {
            throw new QuizException(QuizErrorCode.QUIZ_QUESTION_NOT_IN_SESSION);
        }

        // 옵션 검증
        validateOptions(question, request.getSelectedOptions());

        // 기존 답변 조회
        Optional<QuizAnswer> existing = quizAnswerRepository
                .findBySessionIdAndQuestionId(sessionId, request.getQuestionId());

        QuizAnswer answer;

        if (existing.isPresent()) {
            // 업데이트
            answer = existing.get();
            answer.updateSelectedOptions(request.getSelectedOptions());
        } else {
            // 새로 생성
            answer = QuizAnswer.builder()
                    .session(session)
                    .question(question)
                    .selectedOptions(request.getSelectedOptions())
                    .build();

            quizAnswerRepository.save(answer);
        }

        return QuizAnswerResponse.from(answer);

    }

    private void validateOptions(QuizQuestion question, List<String> selectedOptions) {
        // 질문 null 체크
        if (question == null) {
            throw new QuizException(QuizErrorCode.QUIZ_QUESTION_NOT_FOUND);
        }

        // 선택한 옵션이 null이거나 비어있는지 확인
        if (selectedOptions == null || selectedOptions.isEmpty()) {
            throw new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE,
                    "선택한 옵션이 없습니다");
        }

        // 질문의 옵션이 null이거나 비어있는지 확인
        if (question.getOptions() == null || question.getOptions().isEmpty()) {
            throw new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE,
                    "질문에 옵션이 없습니다: " + question.getId());
        }

        List<String> available = question.getOptions().stream()
                .filter(o -> o != null && o.getValue() != null)
                .map(QuizOption::getValue)
                .toList();

        // 옵션이 유효한지 체크
        for (String option : selectedOptions) {
            if (option == null || !available.contains(option)) {
                throw new QuizException(QuizErrorCode.QUIZ_OPTION_INVALID);
            }
        }

        // 질문 타입별 검증
        QuizType questionType = question.getType();
        if (questionType == QuizType.SINGLE) {
            // SINGLE 타입: 정확히 1개만 선택해야 함
            if (selectedOptions.size() != 1) {
                throw new QuizException(QuizErrorCode.QUIZ_ANSWER_TOO_MANY_OPTIONS);
            }
        } else if (questionType == QuizType.MULTIPLE) {
            // MULTIPLE 타입: 최소 1개 이상 선택해야 함 (이미 위에서 empty 체크했지만 명시적으로)
            if (selectedOptions.size() < 1) {
                throw new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE,
                        "다중 선택 질문에는 최소 1개 이상의 답변이 필요합니다: " + question.getId());
            }
        }
    }

    /**
     * 세션의 퀴즈에 속한 질문 목록 조회
     * - 퀴즈 null 체크 포함
     * - 옵션 포함하여 조회
     */
    private List<QuizQuestion> getQuestionsBySession(QuizSession session) {
        if (session == null) {
            throw new QuizException(QuizErrorCode.SESSION_NOT_FOUND);
        }
        if (session.getQuiz() == null) {
            throw new QuizException(QuizErrorCode.QUIZ_NOT_FOUND);
        }
        return quizQuestionRepository.findAllByQuizIdWithOptions(session.getQuiz().getId());
    }

    /**
     * 세션이 완료되지 않았는지 검증
     */
    private void validateSessionNotCompleted(QuizSession session) {
        if (session == null) {
            throw new QuizException(QuizErrorCode.SESSION_NOT_FOUND);
        }
        if (session.getCompleted()) {
            throw new QuizException(QuizErrorCode.SESSION_ALREADY_COMPLETED);
        }
    }

    /**
     * 만료된 미완료 세션을 삭제합니다.
     *
     * @param expiration 만료 시간
     * @return 삭제된 세션 수
     */
    @Transactional
    public int cleanupExpiredSessions(Duration expiration) {
        LocalDateTime cutoff = LocalDateTime.now().minus(expiration);

        List<QuizSession> expiredSessions =
                quizSessionRepository.findByCompletedFalseAndCreatedAtBefore(cutoff);

        if (expiredSessions.isEmpty()) {
            return 0;
        }

        // CASCADE로 QuizAnswer도 자동 삭제됨 (QuizSession.cascade = CascadeType.ALL, orphanRemoval = true)
        quizSessionRepository.deleteAll(expiredSessions);

        return expiredSessions.size();
    }

}
