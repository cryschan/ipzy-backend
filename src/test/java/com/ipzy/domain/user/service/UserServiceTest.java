package com.ipzy.domain.user.service;

import com.ipzy._global.common.enums.Gender;
import com.ipzy._global.common.enums.UserRole;
import com.ipzy._global.common.enums.UserStatus;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.exception.UserException;
import com.ipzy.domain.user.repository.UserRepository;
import com.ipzy.domain.user.vo.UserStylePreference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@kakao.com")
                .name("tester")
                .phone("010-1234-5678")
                .provider("KAKAO")
                .providerId("123456789")
                .profileImageUrl("https://k.kakaocdn.net/profile.jpg")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("내 프로필 조회")
    class GetMyProfile {

        @Test
        @DisplayName("성공 - 유저 정보를 반환한다")
        void success() {

            // given
            Long userId = 1L;
            given(userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED)).willReturn(Optional.of(user));

            // when
            User result = userService.getMyProfile(userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("test@kakao.com");
            assertThat(result.getName()).isEqualTo("tester");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 유저")
        void fail_userNotFound() {

            // given
            Long userId = 999L;
            given(userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getMyProfile(userId))
                    .isInstanceOf(UserException.class);
        }
    }

    @Nested
    @DisplayName("프로필 수정")
    class UpdateProfile {

        @Test
        @DisplayName("성공 - 프로필 정보를 수정한다")
        void success() {

            // given
            Long userId = 1L;
            String newName = "updated";
            String newPhone = "010-9999-8888";
            String newProfileImageUrl = "https://new-image.jpg";
            given(userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED)).willReturn(Optional.of(user));

            // when
            User result = userService.updateProfile(userId, newName, newPhone, newProfileImageUrl);

            // then
            assertThat(result.getName()).isEqualTo(newName);
            assertThat(result.getPhone()).isEqualTo(newPhone);
            assertThat(result.getProfileImageUrl()).isEqualTo(newProfileImageUrl);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 유저")
        void fail_userNotFound() {

            // given
            Long userId = 999L;
            given(userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.updateProfile(userId, "name", "phone", "url"))
                    .isInstanceOf(UserException.class);
        }
    }

    @Nested
    @DisplayName("환경설정 수정")
    class UpdatePreferences {

        @Test
        @DisplayName("성공 - 환경설정을 수정한다")
        void success() {

            // given
            Long userId = 1L;
            Map<String, Object> newPreferences = new HashMap<>();
            newPreferences.put("theme", "dark");
            newPreferences.put("language", "ko");
            given(userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED)).willReturn(Optional.of(user));

            // when
            User result = userService.updatePreferences(userId, newPreferences);

            // then
            assertThat(result.getPreferences()).containsEntry("theme", "dark");
            assertThat(result.getPreferences()).containsEntry("language", "ko");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 유저")
        void fail_userNotFound() {

            // given
            Long userId = 999L;
            given(userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.updatePreferences(userId, new HashMap<>()))
                    .isInstanceOf(UserException.class);
        }
    }

    @Nested
    @DisplayName("계정 탈퇴")
    class DeleteAccount {

        @Test
        @DisplayName("성공 - 계정을 soft delete 한다")
        void success() {

            // given
            Long userId = 1L;
            given(userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED)).willReturn(Optional.of(user));

            // when
            userService.deleteAccount(userId);

            // then
            assertThat(user.isDeleted()).isTrue();
            assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 유저")
        void fail_userNotFound() {

            // given
            Long userId = 999L;
            given(userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.deleteAccount(userId))
                    .isInstanceOf(UserException.class);
        }

    }

    @Nested
    @DisplayName("스타일 선호도 수정")
    class UpdateStylePreference {

        @Test
        @DisplayName("성공 - 스타일 선호도를 수정한다")
        void success() {

            // given
            Long userId = 1L;
            UserStylePreference stylePreference = UserStylePreference.builder()
                    .colors(List.of("black", "white", "navy"))
                    .age(25)
                    .gender(Gender.MALE)
                    .styles(List.of("casual", "minimal"))
                    .build();
            given(userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED)).willReturn(Optional.of(user));

            // when
            User result = userService.updateStylePreference(userId, stylePreference);

            // then
            assertThat(result.getStylePreference()).isNotNull();
            assertThat(result.getStylePreference().getColors()).containsExactly("black", "white", "navy");
            assertThat(result.getStylePreference().getAge()).isEqualTo(25);
            assertThat(result.getStylePreference().getGender()).isEqualTo(Gender.MALE);
            assertThat(result.getStylePreference().getStyles()).containsExactly("casual", "minimal");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 유저")
        void fail_userNotFound() {

            // given
            Long userId = 999L;
            UserStylePreference stylePreference = UserStylePreference.builder().build();
            given(userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.updateStylePreference(userId, stylePreference))
                    .isInstanceOf(UserException.class);
        }
    }
}
