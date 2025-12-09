package com.ipzy.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "프로필 수정 요청")
public record UpdateProfileRequest(
        @Schema(description = "이름", example = "홍길동")
        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 100, message = "이름은 100자 이하여야 합니다")
        String name,

        @Schema(description = "전화번호", example = "010-1234-5678")
        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
        String phone,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        @Size(max = 500, message = "프로필 이미지 URL은 500자 이하여야 합니다")
        String profileImageUrl
) {
}