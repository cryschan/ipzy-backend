package com.ipzy.domain.quiz.service;

import com.ipzy.domain.quiz.dto.*;
import com.ipzy.domain.quiz.entity.Quiz;
import com.ipzy.domain.quiz.entity.QuizAnswer;
import com.ipzy.domain.quiz.entity.QuizOption;
import com.ipzy.domain.quiz.entity.QuizQuestion;
import com.ipzy.domain.quiz.entity.QuizSession;
import com.ipzy.domain.quiz.exception.QuizErrorCode;
import com.ipzy.domain.quiz.exception.QuizException;
import com.ipzy.domain.quiz.repository.QuizQuestionRepository;
import com.ipzy.domain.quiz.repository.QuizRepository;
import com.ipzy.domain.quiz.repository.QuizSessionRepository;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.repository.UserRepository;
import com.ipzy._global.common.enums.QuizType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final UserRepository userRepository;
    private final QuizQuestionRepository quizQuestionRepository;

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

        // 퀴즈가 null인 경우 예외 처리
        if (session.getQuiz() == null) {
            throw new QuizException(QuizErrorCode.QUIZ_NOT_FOUND);
        }

        // 질문 목록 별도 조회 
        List<QuizQuestion> questions = quizQuestionRepository.findAllByQuizIdWithOptions(
                session.getQuiz().getId());

        // 전체 질문 수
        int totalQuestions = questions.size();
        // 답변한 질문 수
        int answeredCount = session.getAnswers().size();

        // 답변 목록 매핑
        List<QuizAnswerProgressResponse> answerList = session.getAnswers().stream()
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

        // 이미 완료된 세션이면 막기
        if (session.getCompleted()) {
            throw new QuizException(QuizErrorCode.SESSION_ALREADY_COMPLETED);
        }

        // 퀴즈가 null인 경우 예외 처리
        if (session.getQuiz() == null) {
            throw new QuizException(QuizErrorCode.QUIZ_NOT_FOUND);
        }

        // 질문 목록 별도 조회
        List<QuizQuestion> questions = quizQuestionRepository.findAllByQuizIdWithOptions(
                session.getQuiz().getId());

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

        // 1. 필수 질문 답변 여부 확인
        List<Long> answeredQuestionIds = answers.stream()
                .filter(a -> a != null && a.getQuestion() != null)
                .map(a -> a.getQuestion().getId())
                .distinct()
                .toList();

        boolean allRequiredAnswered = questions.stream()
                .filter(QuizQuestion::getRequired)
                .allMatch(q -> answeredQuestionIds.contains(q.getId()));

        if (!allRequiredAnswered) {
            throw new QuizException(QuizErrorCode.QUIZ_REQUIRED_NOT_ANSWERED);
        }

        // 2. 각 답변 내용 검증
        for (QuizAnswer answer : answers) {
            // 답변이 null인 경우 건너뛰기 (이미 위에서 체크했지만 안전을 위해)
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

            // questions 리스트에서 해당 질문 찾기 (options가 로드된 질문 사용)
            QuizQuestion question = questions.stream()
                    .filter(q -> q != null && q.getId() != null && q.getId().equals(questionId))
                    .findFirst()
                    .orElseThrow(() -> new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE,
                            "답변에 해당하는 질문을 찾을 수 없습니다: " + questionId));

            List<String> selectedOptions = answer.getSelectedOptions();

            // 2-1. 선택한 옵션이 비어있지 않은지 확인
            if (selectedOptions == null || selectedOptions.isEmpty()) {
                throw new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE,
                        "질문에 답변이 없습니다: " + question.getId());
            }

            // 2-2. 선택한 옵션이 해당 질문의 유효한 옵션인지 확인
            // 질문의 옵션이 null이거나 비어있는 경우 예외 처리
            if (question.getOptions() == null || question.getOptions().isEmpty()) {
                throw new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE,
                        "질문에 옵션이 없습니다: " + question.getId());
            }

            List<String> validOptionValues = question.getOptions().stream()
                    .filter(o -> o != null && o.getValue() != null)
                    .map(QuizOption::getValue)
                    .toList();

            boolean allOptionsValid = selectedOptions.stream()
                    .allMatch(validOptionValues::contains);

            if (!allOptionsValid) {
                throw new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE,
                        "유효하지 않은 옵션이 선택되었습니다: " + question.getId());
            }

            // 2-3. 질문 타입별 검증
            QuizType questionType = question.getType();
            if (questionType == QuizType.SINGLE) {
                // SINGLE 타입: 정확히 1개만 선택해야 함
                if (selectedOptions.size() != 1) {
                    throw new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE,
                            "단일 선택 질문에는 1개의 답변만 가능합니다: " + question.getId());
                }
            } else if (questionType == QuizType.MULTIPLE) {
                // MULTIPLE 타입: 최소 1개 이상 선택해야 함
                if (selectedOptions.size() < 1) {
                    throw new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE,
                            "다중 선택 질문에는 최소 1개 이상의 답변이 필요합니다: " + question.getId());
                }
            }
        }

        // 3. 중복 답변 체크 (같은 질문에 여러 답변이 있는지)
        long distinctQuestionCount = answers.stream()
                .filter(a -> a != null && a.getQuestion() != null)
                .map(a -> a.getQuestion().getId())
                .distinct()
                .count();

        if (distinctQuestionCount != answers.size()) {
            throw new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE,
                    "같은 질문에 중복 답변이 있습니다");
        }
    }

    @Transactional(readOnly = true)
    public QuizQuestionResponse getQuestionByOrder(Long sessionId, Integer order) {
        // 세션 조회
        QuizSession session = quizSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new QuizException(QuizErrorCode.SESSION_NOT_FOUND));

        // 이미 완료된 세션이면 막기
        if (session.getCompleted()) {
            throw new QuizException(QuizErrorCode.SESSION_ALREADY_COMPLETED);
        }

        // 퀴즈가 null인 경우 예외 처리
        if (session.getQuiz() == null) {
            throw new QuizException(QuizErrorCode.QUIZ_NOT_FOUND);
        }

        // 질문 목록 조회 (JOIN FETCH로 옵션도 함께 조회)
        List<QuizQuestion> questions = quizQuestionRepository.findAllByQuizIdWithOptions(
                session.getQuiz().getId());

        // displayOrder로 질문 찾기
        QuizQuestion question = questions.stream()
                .filter(q -> q != null && q.getDisplayOrder() != null && q.getDisplayOrder().equals(order))
                .findFirst()
                .orElseThrow(() -> new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE,
                        "해당 순서의 질문을 찾을 수 없습니다: " + order));

        // QuizQuestionResponse로 변환
        return QuizQuestionResponse.from(question);
    }

}
