package com.ipzy.domain.auth.handler;

import com.ipzy.domain.auth.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 로그인 실패 시 에러 코드와 함께 프론트엔드로 리다이렉트하는 핸들러
 */
@Slf4j
@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Value("${app.oauth2.failure-redirect-uri}")
    private String failureRedirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        AuthErrorCode errorCode = resolveErrorCode(exception);

        log.error("OAuth2 로그인 실패 - errorCode: {}, message: {}", errorCode.getCode(), exception.getMessage());

        String redirectUrl = UriComponentsBuilder.fromUriString(failureRedirectUri)
                .queryParam("code", errorCode.getCode())
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private AuthErrorCode resolveErrorCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            String errorCode = oauth2Exception.getError().getErrorCode();

            return switch (errorCode) {
                case "access_denied" -> AuthErrorCode.OAUTH_ACCESS_DENIED;
                case "invalid_response" -> AuthErrorCode.OAUTH_INVALID_RESPONSE;
                case "invalid_token" -> AuthErrorCode.OAUTH_INVALID_TOKEN;
                default -> AuthErrorCode.OAUTH_FAILED;
            };
        }

        return AuthErrorCode.OAUTH_FAILED;
    }
}
