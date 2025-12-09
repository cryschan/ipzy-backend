package com.ipzy.domain.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 로그아웃 성공 핸들러.
 *
 * <p>세션 무효화 후 프론트엔드로 리다이렉트합니다.
 * <p>로그인 시 prompt=login 파라미터를 사용하므로 카카오 계정 로그아웃 페이지를 거치지 않아도
 * 재로그인 시 항상 카카오 로그인 화면이 표시됩니다.
 */
@Slf4j
@Component
public class OAuth2LogoutSuccessHandler implements LogoutSuccessHandler {

    private final String logoutRedirectUri;

    public OAuth2LogoutSuccessHandler(
            @Value("${app.oauth2.logout-redirect-uri}") String logoutRedirectUri) {
        this.logoutRedirectUri = logoutRedirectUri;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication) throws IOException {

        if (authentication == null) {
            log.info("비인증 상태에서 로그아웃 요청");
        } else {
            log.info("로그아웃 성공: user={}", authentication.getName());
        }

        // 프론트엔드로 리다이렉트
        log.info("프론트엔드로 리다이렉트: {}", logoutRedirectUri);
        response.sendRedirect(logoutRedirectUri);
    }
}
