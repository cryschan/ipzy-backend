package com.ipzy.domain.user.service;

import com.ipzy._global.common.enums.UserRole;
import com.ipzy._global.common.enums.UserStatus;
import com.ipzy.domain.auth.unlink.OAuth2UnlinkStrategyFactory;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.exception.UserException;
import com.ipzy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    OAuth2UnlinkStrategyFactory unlinkStrategyFactory;

    @InjectMocks
    WithdrawalService withdrawalService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@kakao.com")
                .name("tester")
                .provider("KAKAO")
                .providerId("123456789")
                .profileImageUrl("https://k.kakaocdn.net/profile.jpg")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Nested
    @DisplayName("계정 탈퇴")
    class Withdraw {

        @Test
        @DisplayName("성공 - OAuth 연결 끊고 탈퇴 처리")
        void success_withOAuthUnlink() {
            // given
            Long userId = 1L;
            String provider = "KAKAO";
            String accessToken = "test-access-token";

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(unlinkStrategyFactory.unlink(provider, accessToken)).willReturn(true);

            // when
            withdrawalService.withdraw(userId, provider, accessToken);

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
            verify(unlinkStrategyFactory).unlink(provider, accessToken);
        }

        @Test
        @DisplayName("성공 - OAuth 정보 없이도 탈퇴 가능")
        void success_withoutOAuthInfo() {
            // given
            Long userId = 1L;

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            withdrawalService.withdraw(userId, null, null);

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
            verify(unlinkStrategyFactory, never()).unlink(null, null);
        }

        @Test
        @DisplayName("성공 - OAuth 연결 끊기 실패해도 탈퇴 진행")
        void success_evenWhenUnlinkFails() {
            // given
            Long userId = 1L;
            String provider = "KAKAO";
            String accessToken = "invalid-token";

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(unlinkStrategyFactory.unlink(provider, accessToken)).willReturn(false);

            // when
            withdrawalService.withdraw(userId, provider, accessToken);

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 유저")
        void fail_userNotFound() {
            // given
            Long userId = 999L;
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> withdrawalService.withdraw(userId, "KAKAO", "token"))
                    .isInstanceOf(UserException.class);
        }

        @Test
        @DisplayName("실패 - 이미 탈퇴한 계정")
        void fail_alreadyDeleted() {
            // given
            Long userId = 1L;
            user.withdraw(); // 이미 탈퇴 처리

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> withdrawalService.withdraw(userId, "KAKAO", "token"))
                    .isInstanceOf(UserException.class);
        }

        @Test
        @DisplayName("실패 - 관리자 계정")
        void fail_adminCannotWithdraw() {
            // given
            Long userId = 1L;
            User adminUser = User.builder()
                    .email("admin@ipzy.com")
                    .name("admin")
                    .provider("KAKAO")
                    .providerId("admin123")
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();
            ReflectionTestUtils.setField(adminUser, "id", userId);

            given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));

            // when & then
            assertThatThrownBy(() -> withdrawalService.withdraw(userId, "KAKAO", "token"))
                    .isInstanceOf(UserException.class);
        }
    }
}
