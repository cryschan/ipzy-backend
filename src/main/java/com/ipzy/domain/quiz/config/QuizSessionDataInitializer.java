package com.ipzy.domain.quiz.config;

import com.ipzy.domain.quiz.entity.Quiz;
import com.ipzy.domain.quiz.entity.QuizAnswer;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2) // QuizDataInitializer 이후 실행
public class QuizSessionDataInitializer implements CommandLineRunner {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final UserRepository userRepository;

    @Value("${app.quiz.session.init-mode:SKIP}")
    private String initMode;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            // 초기화 모드에 따른 처리
            InitMode mode = InitMode.fromString(initMode);
            log.info("QuizSession 초기화 모드: {}", mode);

            long existingSessionCount = quizSessionRepository.count();
            
            if (existingSessionCount > 0) {
                switch (mode) {
                    case SKIP:
                        log.info("퀴즈 세션 데이터가 이미 존재합니다 ({}개). SKIP 모드로 초기화를 건너뜁니다.", existingSessionCount);
                        return;
                    case CLEAN:
                        log.info("기존 퀴즈 세션 데이터를 삭제합니다 ({}개)...", existingSessionCount);
                        deleteAllSessions();
                        break;
                    case APPEND:
                        log.info("기존 퀴즈 세션 데이터를 유지하고 추가로 생성합니다 (기존 {}개)...", existingSessionCount);
                        break;
                }
            }

            // 퀴즈가 없으면 초기화 실패
            List<Quiz> quizzes = quizRepository.findAll();
            if (quizzes.isEmpty()) {
                log.error("퀴즈 데이터가 없습니다. QuizDataInitializer를 먼저 실행해주세요.");
                throw new QuizException(QuizErrorCode.QUIZ_SESSION_INIT_QUIZ_NOT_FOUND);
            }

            // 첫 번째 활성화된 퀴즈 사용 (QuizDataInitializer에서 생성한 퀴즈)
            Quiz quiz = quizzes.stream()
                    .filter(Quiz::getIsActive)
                    .findFirst()
                    .orElse(quizzes.get(0)); // 활성화된 퀴즈가 없으면 첫 번째 퀴즈 사용

            log.info("퀴즈 세션 데이터를 초기화합니다... (퀴즈 ID: {})", quiz.getId());

            // 질문 목록 조회 (displayOrder 순서대로)
            List<QuizQuestion> questions = quizQuestionRepository.findAllByQuizIdWithOptions(quiz.getId());
            if (questions.isEmpty()) {
                log.error("퀴즈에 질문이 없습니다. 세션 초기화를 할 수 없습니다. (퀴즈 ID: {})", quiz.getId());
                throw new QuizException(QuizErrorCode.QUIZ_SESSION_INIT_QUESTION_NOT_FOUND);
            }

            // 사용자 조회 (활성 사용자 목록)
            List<User> activeUsers = userRepository.findAll().stream()
                    .filter(u -> u.getDeletedAt() == null)
                    .toList();

            int sessionCount = 0;

            // ========== 완료된 세션들 생성 ==========
            
            // 세션 1: 회원 세션 - 데이트, 깔끔하게, 없음, 30만원
            if (!activeUsers.isEmpty()) {
                QuizSession session1 = createCompletedSession(
                        quiz, activeUsers.get(0), questions,
                        List.of("date"),      // Q1
                        List.of("clean"),     // Q2
                        List.of("none"),      // Q3
                        List.of("300000")     // Q4
                );
                log.info("회원 퀴즈 세션 1 생성 완료 (세션 ID: {})", session1.getId());
                sessionCount++;
            }

            // 세션 2: 비회원 세션 - 데이트, 깔끔하게, 없음, 30만원
            QuizSession session2 = createCompletedSession(
                    quiz, null, questions,
                    List.of("date"),      // Q1
                    List.of("clean"),     // Q2
                    List.of("none"),      // Q3
                    List.of("300000")     // Q4
            );
            log.info("비회원 퀴즈 세션 1 생성 완료 (세션 ID: {})", session2.getId());
            sessionCount++;

            // 세션 3: 회원 세션 - 회사, 멋있게, 마른 편, 50만원
            if (!activeUsers.isEmpty()) {
                User user = activeUsers.size() > 1 ? activeUsers.get(1) : activeUsers.get(0);
                QuizSession session3 = createCompletedSession(
                        quiz, user, questions,
                        List.of("work"),       // Q1
                        List.of("stylish"),    // Q2
                        List.of("thin"),       // Q3
                        List.of("500000")      // Q4
                );
                log.info("회원 퀴즈 세션 2 생성 완료 (세션 ID: {})", session3.getId());
                sessionCount++;
            }

            // 세션 4: 비회원 세션 - 소개팅/모임, 편하게, 통통한 편, 10만원
            QuizSession session4 = createCompletedSession(
                    quiz, null, questions,
                    List.of("meeting"),       // Q1
                    List.of("comfortable"),   // Q2
                    List.of("chubby"),        // Q3
                    List.of("100000")         // Q4
            );
            log.info("비회원 퀴즈 세션 2 생성 완료 (세션 ID: {})", session4.getId());
            sessionCount++;

            // 세션 5: 회원 세션 - 외출, 힙하게, 키, 무관
            if (!activeUsers.isEmpty()) {
                User user = activeUsers.size() > 2 ? activeUsers.get(2) : activeUsers.get(0);
                QuizSession session5 = createCompletedSession(
                        quiz, user, questions,
                        List.of("outdoor"),    // Q1
                        List.of("hip"),        // Q2
                        List.of("height"),     // Q3
                        List.of("unlimited")   // Q4
                );
                log.info("회원 퀴즈 세션 3 생성 완료 (세션 ID: {})", session5.getId());
                sessionCount++;
            }

            // 세션 6: 비회원 세션 - 회사, 깔끔하게, 없음, 10만원
            QuizSession session6 = createCompletedSession(
                    quiz, null, questions,
                    List.of("work"),       // Q1
                    List.of("clean"),     // Q2
                    List.of("none"),      // Q3
                    List.of("100000")      // Q4
            );
            log.info("비회원 퀴즈 세션 3 생성 완료 (세션 ID: {})", session6.getId());
            sessionCount++;

            // 세션 7: 진행 중인 세션 (답변 일부만) - 비회원
            QuizSession session7 = createInProgressSession(quiz, null, questions);
            log.info("진행 중인 비회원 퀴즈 세션 생성 완료 (세션 ID: {})", session7.getId());
            sessionCount++;

            // 세션 8: 진행 중인 세션 (답변 일부만) - 회원
            if (!activeUsers.isEmpty()) {
                QuizSession session8 = createInProgressSession(quiz, activeUsers.get(0), questions);
                log.info("진행 중인 회원 퀴즈 세션 생성 완료 (세션 ID: {})", session8.getId());
                sessionCount++;
            }

            log.info("퀴즈 세션 데이터 초기화가 완료되었습니다. (새로 생성된 세션 {}개)", sessionCount);
        } catch (QuizException e) {
            log.error("퀴즈 세션 초기화 실패: {} (에러 코드: {})", e.getMessage(), e.getErrorCode().getCode(), e);
        } catch (Exception e) {
            log.error("퀴즈 세션 초기화 중 예상치 못한 오류 발생", e);
        }
    }

    /**
     * 모든 QuizSession과 관련 QuizAnswer를 삭제합니다.
     * 
     * CASCADE 설정(cascade = CascadeType.ALL, orphanRemoval = true)으로 인해
     * QuizSession 삭제 시 연관된 QuizAnswer도 자동으로 삭제됩니다.
     */
    private void deleteAllSessions() {
        // CASCADE 설정으로 인해 세션 삭제만으로 충분
        // QuizSession.answers에 cascade = CascadeType.ALL, orphanRemoval = true 설정됨
        quizSessionRepository.deleteAll();
        
        log.info("모든 퀴즈 세션 데이터 삭제 완료 (CASCADE로 답변도 자동 삭제됨)");
    }

    /**
     * 초기화 모드 enum
     */
    private enum InitMode {
        /**
         * 기존 QuizSession 데이터를 모두 삭제하고 새로 생성
         */
        CLEAN,
        
        /**
         * 기존 QuizSession 데이터를 유지하고 추가로 생성
         */
        APPEND,
        
        /**
         * 기존 QuizSession 데이터가 있으면 초기화하지 않음 (기본값)
         */
        SKIP;

        static InitMode fromString(String value) {
            if (value == null || value.isBlank()) {
                return SKIP;
            }
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 알 수 없는 모드는 SKIP으로 처리
                return SKIP;
            }
        }
    }

    /**
     * 완료된 퀴즈 세션을 생성합니다.
     * 
     * @param quiz 퀴즈
     * @param user 사용자 (null이면 비회원 세션)
     * @param questions 질문 목록
     * @param q1Answer Q1 답변 (옵션 value 목록)
     * @param q2Answer Q2 답변 (옵션 value 목록)
     * @param q3Answer Q3 답변 (옵션 value 목록)
     * @param q4Answer Q4 답변 (옵션 value 목록)
     * @return 생성된 세션
     */
    private QuizSession createCompletedSession(Quiz quiz, User user, List<QuizQuestion> questions,
                                                List<String> q1Answer, List<String> q2Answer,
                                                List<String> q3Answer, List<String> q4Answer) {
        // 세션 생성
        QuizSession session = QuizSession.builder()
                .quiz(quiz)
                .user(user)
                .build();
        quizSessionRepository.save(session);

        // 각 질문에 대한 답변 생성
        if (!questions.isEmpty()) {
            createAnswer(session, questions.get(0), q1Answer); // Q1
        }
        if (questions.size() > 1) {
            createAnswer(session, questions.get(1), q2Answer); // Q2
        }
        if (questions.size() > 2) {
            createAnswer(session, questions.get(2), q3Answer); // Q3
        }
        if (questions.size() > 3) {
            createAnswer(session, questions.get(3), q4Answer); // Q4
        }

        // 세션 완료 처리
        session.complete();
        quizSessionRepository.save(session);

        return session;
    }

    /**
     * 진행 중인 퀴즈 세션을 생성합니다 (일부 질문만 답변).
     * 
     * @param quiz 퀴즈
     * @param user 사용자 (null이면 비회원 세션)
     * @param questions 질문 목록
     * @return 생성된 세션
     */
    private QuizSession createInProgressSession(Quiz quiz, User user, List<QuizQuestion> questions) {
        // 세션 생성
        QuizSession session = QuizSession.builder()
                .quiz(quiz)
                .user(user)
                .build();
        quizSessionRepository.save(session);

        // 일부 질문만 답변 (진행 중 상태)
        if (!questions.isEmpty()) {
            createAnswer(session, questions.get(0), List.of("work")); // Q1만 답변
        }
        if (questions.size() > 1) {
            createAnswer(session, questions.get(1), List.of("comfortable")); // Q2만 답변
        }
        // Q3, Q4는 답변하지 않음 (진행 중)

        // 세션은 완료하지 않음 (completed = false)
        return session;
    }

    /**
     * 답변을 생성합니다.
     * 
     * @param session 세션
     * @param question 질문
     * @param selectedOptionValues 선택한 옵션 value 목록
     */
    private void createAnswer(QuizSession session, QuizQuestion question, List<String> selectedOptionValues) {
        QuizAnswer answer = QuizAnswer.builder()
                .session(session)
                .question(question)
                .selectedOptions(selectedOptionValues)
                .build();
        quizAnswerRepository.save(answer);
    }
}

