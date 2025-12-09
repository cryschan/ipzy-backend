package com.ipzy._global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ipzy._global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API 공통 응답 래퍼 - 성공/실패 응답을 통일된 형식으로 반환
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API 공통 응답")
public class ApiResponse<T> {

    @Schema(description = "요청 성공 여부", example = "true")
    private final boolean success;

    @Schema(description = "응답 데이터 (성공 시)")
    private final T data;

    @Schema(description = "에러 정보 (실패 시)")
    private final ErrorInfo error;

    // 성공 응답
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    // 에러 응답
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, ErrorInfo.of(errorCode));
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, ErrorInfo.of(errorCode.getCode(), message));
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, ErrorInfo.of(code, message));
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Schema(description = "에러 정보")
    public static class ErrorInfo {
        @Schema(description = "에러 코드", example = "AUTH_001")
        private final String code;

        @Schema(description = "에러 메시지", example = "인증이 필요합니다")
        private final String message;

        public static ErrorInfo of(ErrorCode errorCode) {
            return new ErrorInfo(errorCode.getCode(), errorCode.getMessage());
        }

        public static ErrorInfo of(String code, String message) {
            return new ErrorInfo(code, message);
        }
    }
}
