package com.ipzy.domain.quiz.exception;

import com.ipzy._global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 퀴즈 관련 에러 코드 (QUIZ_001 ~ QUIZ_017)
 *
 * 에러 코드 분류:
 * - 퀴즈 관련: QUIZ_001, QUIZ_006
 * - 세션 관련: QUIZ_003, QUIZ_004, QUIZ_017
 * - 질문 관련: QUIZ_007, QUIZ_008
 * - 답변 관련: QUIZ_005, QUIZ_010, QUIZ_013, QUIZ_014
 * - 옵션 관련: QUIZ_009
 * - 검증 관련: QUIZ_002
 * - 초기화 관련: QUIZ_011, QUIZ_012, QUIZ_015, QUIZ_016
 */
@Getter
@RequiredArgsConstructor
public enum QuizErrorCode implements ErrorCode {

    // ========== 퀴즈 관련 ==========
    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "QUIZ_001", "퀴즈를 찾을 수 없습니다"),
    QUIZ_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "QUIZ_006", "퀴즈가 완료되지 않았습니다"),

    // ========== 세션 관련 ==========
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "QUIZ_003", "퀴즈 세션을 찾을 수 없습니다"),
    SESSION_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "QUIZ_004", "이미 완료된 퀴즈 세션입니다"),
    SESSION_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "QUIZ_017", "이미 사용자가 연결된 세션입니다"),

    // ========== 질문 관련 ==========
    QUIZ_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "QUIZ_007", "퀴즈 질문을 찾을 수 없습니다"),
    QUIZ_QUESTION_NOT_IN_SESSION(HttpStatus.BAD_REQUEST, "QUIZ_008", "해당 질문이 세션의 퀴즈에 속하지 않습니다"),

    // ========== 답변 관련 ==========
    QUIZ_REQUIRED_NOT_ANSWERED(HttpStatus.BAD_REQUEST, "QUIZ_005", "필수 질문에 답변하지 않았습니다"),
    QUIZ_ANSWER_TOO_MANY_OPTIONS(HttpStatus.BAD_REQUEST, "QUIZ_010", "단일 선택 질문에는 1개의 옵션만 선택할 수 있습니다"),
    QUIZ_ANSWER_NULL(HttpStatus.BAD_REQUEST, "QUIZ_013", "답변이 저장되지 않았습니다"),
    QUIZ_ANSWER_QUESTION_NULL(HttpStatus.BAD_REQUEST, "QUIZ_014", "답변에 해당하는 질문이 없습니다"),

    // ========== 옵션 관련 ==========
    QUIZ_OPTION_INVALID(HttpStatus.BAD_REQUEST, "QUIZ_009", "유효하지 않은 옵션입니다"),

    // ========== 검증 관련 ==========
    INVALID_QUIZ_RESPONSE(HttpStatus.BAD_REQUEST, "QUIZ_002", "유효하지 않은 퀴즈 응답입니다"),

    // ========== 초기화 관련 ==========
    QUIZ_SESSION_INIT_QUIZ_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "QUIZ_011", "퀴즈 세션 초기화 실패: 퀴즈 데이터가 없습니다"),
    QUIZ_SESSION_INIT_QUESTION_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "QUIZ_012", "퀴즈 세션 초기화 실패: 퀴즈에 질문이 없습니다"),
    QUIZ_SESSION_INIT_MODE_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "QUIZ_015", "퀴즈 세션 초기화 모드가 유효하지 않습니다"),
    QUIZ_SESSION_INIT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "QUIZ_016", "퀴즈 세션 초기화 중 예상치 못한 오류가 발생했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
