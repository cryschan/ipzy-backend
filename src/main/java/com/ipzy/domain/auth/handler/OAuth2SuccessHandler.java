package com.ipzy.domain.auth.handler;

import com.ipzy.domain.auth.dto.CustomUserPrincipal;
import com.ipzy.domain.auth.logout.OAuth2TokenSessionKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 로그인 성공 시 프론트엔드로 리다이렉트하는 핸들러.
 *
 * <p>Provider별로 access_token을 세션에 저장하고 메인 화면으로 리다이렉트합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.oauth2.success-redirect-uri}")
    private String successRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        // Provider별 access_token 세션 저장 (로그아웃 시 API 호출용)
        saveAccessTokenToSession(request, authentication);

        log.info("OAuth2 로그인 성공 - userId: {}", principal.getUserId());

        response.sendRedirect(successRedirectUri);
    }

    /**
     * OAuth2 access_token을 Provider별 세션 키로 저장합니다.
     */
    private void saveAccessTokenToSession(HttpServletRequest request, Authentication authentication) {

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {

            String registrationId = oauthToken.getAuthorizedClientRegistrationId();
            String provider = registrationId.toUpperCase();

            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    registrationId,
                    oauthToken.getName()
            );

            if (client != null && client.getAccessToken() != null) {

                String accessToken = client.getAccessToken().getTokenValue();
                String sessionKey = OAuth2TokenSessionKey.getSessionKey(provider);

                request.getSession().setAttribute(sessionKey, accessToken);

                // Provider 정보도 세션에 저장 (로그아웃 시 사용)
                request.getSession().setAttribute("OAUTH2_PROVIDER", provider);

                log.debug("{} access_token 세션 저장 완료 (key: {})", provider, sessionKey);
            }
        }
    }
}
