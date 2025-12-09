package com.ipzy.domain.user.dto;

import com.ipzy._global.common.enums.Gender;
import com.ipzy.domain.user.vo.UserStylePreference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "스타일 선호도 수정 요청")
public record UpdateStylePreferenceRequest(
        @Schema(description = "선호 색상", example = "[\"black\", \"white\", \"navy\"]")
        @Size(max = 10, message = "선호 색상은 최대 10개까지 선택 가능합니다")
        List<String> colors,

        @Schema(description = "나이", example = "25")
        @Min(value = 1, message = "나이는 1 이상이어야 합니다")
        @Max(value = 150, message = "나이는 150 이하여야 합니다")
        Integer age,

        @Schema(description = "성별", example = "MALE")
        Gender gender,

        @Schema(description = "선호 스타일", example = "[\"casual\", \"minimal\"]")
        @Size(max = 10, message = "선호 스타일은 최대 10개까지 선택 가능합니다")
        List<String> styles
) {

    public UserStylePreference toVo() {
        return UserStylePreference.builder()
                .colors(colors)
                .age(age)
                .gender(gender)
                .styles(styles)
                .build();
    }
}
