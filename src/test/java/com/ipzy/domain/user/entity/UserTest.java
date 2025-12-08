package com.ipzy.domain.user.entity;

import com.ipzy._global.common.enums.UserRole;
import com.ipzy._global.common.enums.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Nested
    @DisplayName("User 생성")
    class Create {

        @Test
        @DisplayName("필수 필드로 User 생성 시 기본값이 설정된다")
        void createWithRequiredFields() {
            User user = User.builder()
                    .email("test@example.com")
                    .name("테스트")
                    .provider("KAKAO")
                    .providerId("12345")
                    .build();

            assertThat(user.getEmail()).isEqualTo("test@example.com");
            assertThat(user.getName()).isEqualTo("테스트");
            assertThat(user.getRole()).isEqualTo(UserRole.USER);
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.getPreferences()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("모든 필드로 User 생성")
        void createWithAllFields() {
            Map<String, Object> preferences = Map.of("theme", "dark");

            User user = User.builder()
                    .email("test@example.com")
                    .name("테스트")
                    .phone("010-1234-5678")
                    .provider("KAKAO")
                    .providerId("12345")
                    .profileImageUrl("https://example.com/image.png")
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .preferences(preferences)
                    .build();

            assertThat(user.getPhone()).isEqualTo("010-1234-5678");
            assertThat(user.getProfileImageUrl()).isEqualTo("https://example.com/image.png");
            assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
            assertThat(user.getPreferences()).containsEntry("theme", "dark");
        }
    }

    @Nested
    @DisplayName("상태 변경")
    class StatusChange {

        @Test
        @DisplayName("suspend() 호출 시 SUSPENDED 상태로 변경")
        void suspend() {
            User user = createActiveUser();

            user.suspend();

            assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
            assertThat(user.isActive()).isFalse();
        }

        @Test
        @DisplayName("delete() 호출 시 DELETED 상태로 변경되고 deletedAt 설정")
        void delete() {
            User user = createActiveUser();

            user.delete();

            assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
            assertThat(user.isDeleted()).isTrue();
            assertThat(user.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("activate() 호출 시 ACTIVE 상태로 변경되고 deletedAt 초기화")
        void activate() {
            User user = createActiveUser();
            user.delete();

            user.activate();

            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.isActive()).isTrue();
            assertThat(user.getDeletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("프로필 업데이트")
    class ProfileUpdate {

        @Test
        @DisplayName("updateProfile() 호출 시 프로필 정보 변경")
        void updateProfile() {
            User user = createActiveUser();

            user.updateProfile("새이름", "010-9999-9999", "https://new-image.com/img.png");

            assertThat(user.getName()).isEqualTo("새이름");
            assertThat(user.getPhone()).isEqualTo("010-9999-9999");
            assertThat(user.getProfileImageUrl()).isEqualTo("https://new-image.com/img.png");
        }

        @Test
        @DisplayName("updatePreferences() 호출 시 설정 변경")
        void updatePreferences() {
            User user = createActiveUser();
            Map<String, Object> newPreferences = Map.of("language", "ko", "notifications", true);

            user.updatePreferences(newPreferences);

            assertThat(user.getPreferences())
                    .containsEntry("language", "ko")
                    .containsEntry("notifications", true);
        }

        @Test
        @DisplayName("updateLastLoginAt() 호출 시 마지막 로그인 시간 갱신")
        void updateLastLoginAt() {
            User user = createActiveUser();
            assertThat(user.getLastLoginAt()).isNull();

            user.updateLastLoginAt();

            assertThat(user.getLastLoginAt()).isNotNull();
        }
    }

    private User createActiveUser() {
        return User.builder()
                .email("test@example.com")
                .name("테스트")
                .provider("KAKAO")
                .providerId("12345")
                .build();
    }
}
