package com.ipzy.domain.auth.handler;

import com.ipzy.domain.auth.logout.OAuth2LogoutStrategyFactory;
import com.ipzy.domain.auth.logout.OAuth2TokenSessionKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 로그아웃 성공 핸들러.
 *
 * <p>Strategy 패턴으로 Provider별 로그아웃 API를 호출하고 JSON 응답을 반환합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LogoutSuccessHandler implements LogoutSuccessHandler {

    private final OAuth2LogoutStrategyFactory strategyFactory;

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication) throws IOException {

        if (authentication == null) {
            log.info("비인증 상태에서 로그아웃 요청");
        } else {
            log.info("로그아웃 성공: user={}", authentication.getName());
            // Provider별 토큰 revoke
            revokeProviderToken(request);
        }

        // JSON 응답 반환
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"success\":true,\"data\":{\"message\":\"로그아웃 되었습니다\"}}"
        );
    }

    /**
     * Provider별 access_token revoke API를 호출합니다.
     */
    private void revokeProviderToken(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            log.warn("세션이 없습니다");
            return;
        }

        // 세션에서 Provider 정보 조회
        String provider = (String) session.getAttribute("OAUTH2_PROVIDER");

        if (provider == null) {
            log.warn("세션에 Provider 정보가 없습니다");
            return;
        }

        // 세션에서 access_token 조회
        String sessionKey = OAuth2TokenSessionKey.getSessionKey(provider);
        String accessToken = (String) session.getAttribute(sessionKey);

        if (accessToken == null) {
            log.warn("세션에 {} access_token이 없습니다", provider);
            return;
        }

        // Strategy로 토큰 revoke
        strategyFactory.getStrategy(provider)
                .ifPresentOrElse(
                        strategy -> strategy.revokeToken(accessToken),
                        () -> log.warn("{}에 대한 로그아웃 전략이 없습니다", provider)
                );
    }
}
