package com.ipzy.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "퀴즈 통계 응답")
public record AdminQuizStatsResponse(
    @Schema(description = "총 응답 수", example = "12456")
    Long totalResponses,

    @Schema(description = "질문 목록")
    List<QuestionStat> questions
) {
    @Schema(description = "질문별 통계")
    public record QuestionStat(
        @Schema(description = "질문 ID", example = "1")
        Long id,

        @Schema(description = "질문 텍스트", example = "어디 가요?")
        String question,

        @Schema(description = "옵션별 통계")
        List<OptionStat> options
    ) {
    }

    @Schema(description = "옵션별 통계")
    public record OptionStat(
        @Schema(description = "옵션 값", example = "school")
        String value,

        @Schema(description = "옵션 라벨", example = "학교")
        String label,

        @Schema(description = "선택 횟수", example = "1121")
        Long count,

        @Schema(description = "선택 비율 (%)", example = "9")
        Integer percentage
    ) {
    }

    public static AdminQuizStatsResponse of(Long totalResponses, List<QuestionStat> questions) {
        return new AdminQuizStatsResponse(totalResponses, questions);
    }
}
