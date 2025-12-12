package com.ipzy.domain.quiz.exception;

import com.ipzy._global.exception.BusinessException;
import com.ipzy._global.exception.ErrorCode;

/**
 * 퀴즈 관련 예외 (팩토리 메서드로 생성)
 */
public class QuizException extends BusinessException {

    private QuizException(ErrorCode errorCode) {
        super(errorCode);
    }

    private QuizException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    // ========== 퀴즈 관련 ==========

    public static QuizException quizNotFound() {
        return new QuizException(QuizErrorCode.QUIZ_NOT_FOUND);
    }

    public static QuizException invalidResponse() {
        return new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE);
    }

    public static QuizException invalidResponse(String message) {
        return new QuizException(QuizErrorCode.INVALID_QUIZ_RESPONSE, message);
    }

    public static QuizException requiredNotAnswered() {
        return new QuizException(QuizErrorCode.QUIZ_REQUIRED_NOT_ANSWERED);
    }

    public static QuizException notCompleted() {
        return new QuizException(QuizErrorCode.QUIZ_NOT_COMPLETED);
    }

    public static QuizException notCompleted(String message) {
        return new QuizException(QuizErrorCode.QUIZ_NOT_COMPLETED, message);
    }

    // ========== 세션 관련 ==========

    public static QuizException sessionNotFound() {
        return new QuizException(QuizErrorCode.SESSION_NOT_FOUND);
    }

    public static QuizException sessionAlreadyCompleted() {
        return new QuizException(QuizErrorCode.SESSION_ALREADY_COMPLETED);
    }

    // ========== 질문 관련 ==========

    public static QuizException questionNotFound() {
        return new QuizException(QuizErrorCode.QUIZ_QUESTION_NOT_FOUND);
    }

    public static QuizException questionNotInSession() {
        return new QuizException(QuizErrorCode.QUIZ_QUESTION_NOT_IN_SESSION);
    }

    // ========== 옵션/답변 관련 ==========

    public static QuizException optionInvalid() {
        return new QuizException(QuizErrorCode.QUIZ_OPTION_INVALID);
    }

    public static QuizException tooManyOptions() {
        return new QuizException(QuizErrorCode.QUIZ_ANSWER_TOO_MANY_OPTIONS);
    }
}
