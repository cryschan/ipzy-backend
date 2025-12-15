package com.ipzy.domain.user.dto;

/**
 * 프로필 수정 Service 명령 객체.
 *
 * <p>Controller의 Request DTO와 Service 계층을 분리합니다.
 */
public record UpdateProfileCommand(
        String name,
        String phone,
        String profileImageUrl
) {
    public static UpdateProfileCommand from(UpdateProfileRequest request) {
        return new UpdateProfileCommand(
                request.name(),
                request.phone(),
                request.profileImageUrl()
        );
    }
}
