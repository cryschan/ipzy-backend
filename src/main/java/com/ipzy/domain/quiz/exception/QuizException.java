package com.ipzy.domain.quiz.exception;

import com.ipzy.global.exception.BusinessException;

public class QuizException extends BusinessException {

    public QuizException(QuizErrorCode errorCode) {
        super(errorCode);
    }

    public QuizException(QuizErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
