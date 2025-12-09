package com.ipzy.domain.auth.session;

import com.ipzy.domain.auth.logout.OAuth2TokenSessionKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * HTTP 세션에서 OAuth2 토큰 정보를 조회하는 헬퍼.
 *
 * <p>Controller에서 세션 조회 로직을 분리하여 테스트 용이성을 높입니다.
 */
@Component
public class OAuth2SessionTokenResolver {

    /**
     * 세션에서 OAuth2 Provider와 Access Token을 조회합니다.
     *
     * @param request HTTP 요청
     * @return OAuth2 토큰 정보 (없으면 empty)
     */
    public Optional<OAuth2SessionToken> resolve(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        String provider = (String) session.getAttribute("OAUTH2_PROVIDER");
        if (provider == null) {
            return Optional.empty();
        }

        String sessionKey = OAuth2TokenSessionKey.getSessionKey(provider);
        String accessToken = (String) session.getAttribute(sessionKey);

        return Optional.of(new OAuth2SessionToken(provider, accessToken));
    }

    /**
     * 세션을 무효화합니다.
     *
     * @param request HTTP 요청
     */
    public void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /**
     * OAuth2 세션 토큰 정보를 담는 레코드.
     */
    public record OAuth2SessionToken(String provider, String accessToken) {
    }
}
