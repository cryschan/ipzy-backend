package com.ipzy.domain.user.dto;

import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.vo.UserStylePreference;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

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

        @Schema(description = "사용자 선호도 (스타일, 색상)")
        PreferencesDto preferences
) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getProfileImageUrl(),
                PreferencesDto.from(user.getStylePreference())
        );
    }

    @Schema(description = "사용자 선호도")
    public record PreferencesDto(
            @Schema(description = "선호 스타일", example = "[\"casual\", \"minimal\"]")
            List<String> style,

            @Schema(description = "선호 색상", example = "[\"black\", \"white\", \"navy\"]")
            List<String> colors
    ) {
        public static PreferencesDto from(UserStylePreference preference) {
            if (preference == null) {
                return new PreferencesDto(List.of(), List.of());
            }
            return new PreferencesDto(
                    preference.getStyles() != null ? preference.getStyles() : List.of(),
                    preference.getColors() != null ? preference.getColors() : List.of()
            );
        }
    }
}
