package com.ipzy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "환경설정 수정 요청")
public record UpdatePreferencesRequest(
        @Schema(description = "환경설정", example = "{\"theme\": \"dark\", \"language\": \"ko\", \"notifications\": {\"push\": true}}")
        @NotNull(message = "환경설정은 필수입니다")
        Map<String, Object> preferences
) {
}