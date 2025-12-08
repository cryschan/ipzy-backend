package com.ipzy.domain.auth.handler;

import com.ipzy.domain.auth.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 로그아웃 성공 시 카카오 로그아웃 후 프론트엔드로 리다이렉트하는 핸들러
 */
@Slf4j
@Component
public class OAuth2LogoutSuccessHandler implements LogoutSuccessHandler {

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${app.oauth2.logout-redirect-uri}")
    private String logoutRedirectUri;

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication) throws IOException {
        // 비로그인 상태에서 로그아웃 요청 시 401 응답
        if (authentication == null) {
            log.warn("비인증 상태에서 로그아웃 요청");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                String.format("{\"success\":false,\"error\":{\"code\":\"%s\",\"message\":\"%s\"}}",
                    AuthErrorCode.UNAUTHORIZED.getCode(),
                    AuthErrorCode.UNAUTHORIZED.getMessage())
            );
            return;
        }

        log.info("로그아웃 성공: user={}", authentication.getName());

        // 카카오 로그아웃 URL로 리다이렉트
        String encodedRedirectUri = URLEncoder.encode(logoutRedirectUri, StandardCharsets.UTF_8);
        String kakaoLogoutUrl = String.format(
            "https://kauth.kakao.com/oauth/logout?client_id=%s&logout_redirect_uri=%s",
            kakaoClientId,
            encodedRedirectUri
        );

        log.debug("카카오 로그아웃 리다이렉트: {}", kakaoLogoutUrl);
        response.sendRedirect(kakaoLogoutUrl);
    }
}
