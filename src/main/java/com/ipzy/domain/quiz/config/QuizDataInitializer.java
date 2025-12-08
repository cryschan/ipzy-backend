package com.ipzy.domain.quiz.config;

import com.ipzy.domain.quiz.entity.Quiz;
import com.ipzy.domain.quiz.entity.QuizOption;
import com.ipzy.domain.quiz.entity.QuizQuestion;
import com.ipzy.domain.quiz.repository.QuizRepository;
import com.ipzy.global.common.enums.QuizType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizDataInitializer implements CommandLineRunner {

    private final QuizRepository quizRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // 이미 데이터가 있으면 초기화하지 않음
        if (quizRepository.count() > 0) {
            log.info("퀴즈 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("퀴즈 기본 데이터를 초기화합니다...");

        // 퀴즈 생성
        Quiz quiz = Quiz.builder()
                .title("나의 스타일 찾기")
                .description("4가지 질문으로 당신의 스타일을 분석합니다")
                .isActive(true)
                .displayOrder(1)
                .build();

        // Q1. 어디 가요?
        QuizQuestion question1 = QuizQuestion.builder()
                .quiz(quiz)
                .text("어디 가요?")
                .type(QuizType.SINGLE)
                .displayOrder(1)
                .required(true)
                .build();

        question1.addOption(QuizOption.builder()
                .question(question1)
                .text("회사")
                .value("work")
                .displayOrder(1)
                .build());
        question1.addOption(QuizOption.builder()
                .question(question1)
                .text("데이트")
                .value("date")
                .displayOrder(2)
                .build());
        question1.addOption(QuizOption.builder()
                .question(question1)
                .text("소개팅/모임")
                .value("meeting")
                .displayOrder(3)
                .build());
        question1.addOption(QuizOption.builder()
                .question(question1)
                .text("외출")
                .value("outdoor")
                .displayOrder(4)
                .build());

        // Q2. 어떻게 보이고 싶어요?
        QuizQuestion question2 = QuizQuestion.builder()
                .quiz(quiz)
                .text("어떻게 보이고 싶어요?")
                .type(QuizType.SINGLE)
                .displayOrder(2)
                .required(true)
                .build();

        question2.addOption(QuizOption.builder()
                .question(question2)
                .text("깔끔하게")
                .value("clean")
                .displayOrder(1)
                .build());
        question2.addOption(QuizOption.builder()
                .question(question2)
                .text("편하게")
                .value("comfortable")
                .displayOrder(2)
                .build());
        question2.addOption(QuizOption.builder()
                .question(question2)
                .text("멋있게")
                .value("stylish")
                .displayOrder(3)
                .build());
        question2.addOption(QuizOption.builder()
                .question(question2)
                .text("힙하게")
                .value("hip")
                .displayOrder(4)
                .build());

        // Q3. 체형 고민?
        QuizQuestion question3 = QuizQuestion.builder()
                .quiz(quiz)
                .text("체형 고민?")
                .type(QuizType.SINGLE)
                .displayOrder(3)
                .required(true)
                .build();

        question3.addOption(QuizOption.builder()
                .question(question3)
                .text("없음")
                .value("none")
                .displayOrder(1)
                .build());
        question3.addOption(QuizOption.builder()
                .question(question3)
                .text("통통한 편")
                .value("chubby")
                .displayOrder(2)
                .build());
        question3.addOption(QuizOption.builder()
                .question(question3)
                .text("마른 편")
                .value("thin")
                .displayOrder(3)
                .build());
        question3.addOption(QuizOption.builder()
                .question(question3)
                .text("키")
                .value("height")
                .displayOrder(4)
                .build());

        // Q4. 예산은?
        QuizQuestion question4 = QuizQuestion.builder()
                .quiz(quiz)
                .text("예산은?")
                .type(QuizType.SINGLE)
                .displayOrder(4)
                .required(true)
                .build();

        question4.addOption(QuizOption.builder()
                .question(question4)
                .text("10만원")
                .value("100000")
                .displayOrder(1)
                .build());
        question4.addOption(QuizOption.builder()
                .question(question4)
                .text("30만원")
                .value("300000")
                .displayOrder(2)
                .build());
        question4.addOption(QuizOption.builder()
                .question(question4)
                .text("50만원")
                .value("500000")
                .displayOrder(3)
                .build());
        question4.addOption(QuizOption.builder()
                .question(question4)
                .text("무관")
                .value("unlimited")
                .displayOrder(4)
                .build());

        // 퀴즈에 질문 추가
        quiz.addQuestion(question1);
        quiz.addQuestion(question2);
        quiz.addQuestion(question3);
        quiz.addQuestion(question4);

        // 저장
        quizRepository.save(quiz);

        log.info("퀴즈 기본 데이터 초기화가 완료되었습니다. (퀴즈 1개, 질문 4개, 옵션 16개)");
    }
}

