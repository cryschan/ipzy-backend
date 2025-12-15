package com.ipzy.domain.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.io.IOException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuth2LogoutSuccessHandlerTest {

    private static final String LOGOUT_REDIRECT_URI = "http://localhost:5173";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    private OAuth2LogoutSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2LogoutSuccessHandler(LOGOUT_REDIRECT_URI);
    }

    @Nested
    @DisplayName("onLogoutSuccess")
    class OnLogoutSuccess {

        @Test
        @DisplayName("인증된 사용자 로그아웃 시 프론트엔드로 리다이렉트")
        void redirectsToFrontend_whenAuthenticated() throws IOException {
            // given
            given(authentication.getName()).willReturn("test@kakao.com");

            // when
            handler.onLogoutSuccess(request, response, authentication);

            // then
            verify(response).sendRedirect(LOGOUT_REDIRECT_URI);
        }

        @Test
        @DisplayName("비인증 상태 로그아웃 시에도 프론트엔드로 리다이렉트")
        void redirectsToFrontend_whenNotAuthenticated() throws IOException {
            // given
            // authentication is null

            // when
            handler.onLogoutSuccess(request, response, null);

            // then
            verify(response).sendRedirect(LOGOUT_REDIRECT_URI);
        }
    }
}
