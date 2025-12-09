package com.ipzy.domain.user.dto;

import com.ipzy._global.common.enums.Gender;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.vo.UserStylePreference;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "사용자 프로필 응답")
public record UserProfileResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "이메일", example = "user@kakao.com")
        String email,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "전화번호", example = "010-1234-5678")
        String phone,

        @Schema(description = "프로필 이미지 URL", example = "https://k.kakaocdn.net/profile.jpg")
        String profileImageUrl,

        @Schema(description = "앱 환경설정")
        Map<String, Object> preferences,

        @Schema(description = "스타일 선호도")
        StylePreferenceDto stylePreference
) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getProfileImageUrl(),
                user.getPreferences(),
                StylePreferenceDto.from(user.getStylePreference())
        );
    }

    @Schema(description = "스타일 선호도")
    public record StylePreferenceDto(
            @Schema(description = "선호 색상", example = "[\"black\", \"white\", \"navy\"]")
            List<String> colors,

            @Schema(description = "나이", example = "25")
            Integer age,

            @Schema(description = "성별", example = "MALE")
            Gender gender,

            @Schema(description = "선호 스타일", example = "[\"casual\", \"minimal\"]")
            List<String> styles
    ) {
        public static StylePreferenceDto from(UserStylePreference preference) {
            if (preference == null) {
                return null;
            }
            return new StylePreferenceDto(
                    preference.getColors(),
                    preference.getAge(),
                    preference.getGender(),
                    preference.getStyles()
            );
        }
    }
}
