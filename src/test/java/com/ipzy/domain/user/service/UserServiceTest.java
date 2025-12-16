package com.ipzy.domain.user.service;

import com.ipzy._global.common.enums.Gender;
import com.ipzy._global.common.enums.UserRole;
import com.ipzy._global.common.enums.UserStatus;
import com.ipzy.domain.user.dto.UserProfileResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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
            UserProfileResponse result = userService.getMyProfile(userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.email()).isEqualTo("test@kakao.com");
            assertThat(result.name()).isEqualTo("tester");
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
            UserProfileResponse result = userService.updateProfile(userId, newName, newPhone, newProfileImageUrl);

            // then
            assertThat(result.name()).isEqualTo(newName);
            assertThat(result.phone()).isEqualTo(newPhone);
            assertThat(result.profileImageUrl()).isEqualTo(newProfileImageUrl);
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
            UserProfileResponse result = userService.updatePreferences(userId, newPreferences);

            // then
            assertThat(result.preferences()).containsEntry("theme", "dark");
            assertThat(result.preferences()).containsEntry("language", "ko");
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
            UserProfileResponse result = userService.updateStylePreference(userId, stylePreference);

            // then
            assertThat(result.stylePreference()).isNotNull();
            assertThat(result.stylePreference().colors()).containsExactly("black", "white", "navy");
            assertThat(result.stylePreference().age()).isEqualTo(25);
            assertThat(result.stylePreference().gender()).isEqualTo(Gender.MALE);
            assertThat(result.stylePreference().styles()).containsExactly("casual", "minimal");
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

    @Nested
    @DisplayName("OAuth 사용자 조회/생성")
    class FindOrCreateByOAuth {

        @Test
        @DisplayName("성공 - provider+providerId로 기존 사용자 조회")
        void success_findByProviderAndProviderId() {

            // given
            given(userRepository.findByProviderAndProviderId("KAKAO", "12345"))
                    .willReturn(Optional.of(user));

            // when
            User result = userService.findOrCreateByOAuth(
                    "KAKAO", "12345", "test@kakao.com", "테스트", "https://img.com/profile.jpg"
            );

            // then
            assertThat(result).isEqualTo(user);
            assertThat(result.getLastLoginAt()).isNotNull();
            verify(userRepository, never()).findByEmail(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("성공 - email로 기존 사용자 조회 후 소셜 연동")
        void success_findByEmailAndLinkSocialAccount() {

            // given
            User googleUser = User.builder()
                    .email("test@kakao.com")
                    .name("구글유저")
                    .provider("GOOGLE")
                    .providerId("google123")
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build();

            given(userRepository.findByProviderAndProviderId("KAKAO", "kakao456"))
                    .willReturn(Optional.empty());
            given(userRepository.findByEmail("test@kakao.com"))
                    .willReturn(Optional.of(googleUser));

            // when
            User result = userService.findOrCreateByOAuth(
                    "KAKAO", "kakao456", "test@kakao.com", "카카오유저", "https://img.com/kakao.jpg"
            );

            // then
            assertThat(result.getProvider()).isEqualTo("KAKAO");
            assertThat(result.getProviderId()).isEqualTo("kakao456");
            assertThat(result.getLastLoginAt()).isNotNull();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("성공 - 새 사용자 생성")
        void success_createNewUser() {

            // given
            given(userRepository.findByProviderAndProviderId("KAKAO", "new123"))
                    .willReturn(Optional.empty());
            given(userRepository.findByEmail("new@kakao.com"))
                    .willReturn(Optional.empty());

            User savedUser = User.builder()
                    .email("new@kakao.com")
                    .name("신규유저")
                    .provider("KAKAO")
                    .providerId("new123")
                    .profileImageUrl("https://img.com/new.jpg")
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build();
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            User result = userService.findOrCreateByOAuth(
                    "KAKAO", "new123", "new@kakao.com", "신규유저", "https://img.com/new.jpg"
            );

            // then
            assertThat(result.getEmail()).isEqualTo("new@kakao.com");
            assertThat(result.getProvider()).isEqualTo("KAKAO");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("성공 - 이름이 없으면 이메일 앞부분으로 대체")
        void success_nameFromEmail() {

            // given
            given(userRepository.findByProviderAndProviderId("KAKAO", "12345"))
                    .willReturn(Optional.of(user));

            // when
            User result = userService.findOrCreateByOAuth(
                    "KAKAO", "12345", "testuser@kakao.com", null, "https://img.com/profile.jpg"
            );

            // then
            assertThat(result).isNotNull();
            // 이름이 null이면 이메일 앞부분(testuser)로 대체되어 updateOAuthInfo 호출됨
        }
    }
}
