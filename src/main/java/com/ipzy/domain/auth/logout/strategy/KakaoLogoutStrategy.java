package com.ipzy.domain.auth.logout.strategy;

import com.ipzy.domain.auth.logout.OAuth2LogoutStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 카카오 로그아웃 전략.
 *
 * <p>API: POST https://kapi.kakao.com/v1/user/logout
 * <p>인증: Authorization: Bearer {access_token}
 */
@Slf4j
@Component
public class KakaoLogoutStrategy implements OAuth2LogoutStrategy {

    private static final String LOGOUT_URL = "https://kapi.kakao.com/v1/user/logout";
    private final RestTemplate restTemplate;

    public KakaoLogoutStrategy() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String getProvider() {
        return "KAKAO";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void revokeToken(String accessToken) {

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<Map> response = restTemplate.exchange(
                    LOGOUT_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(headers),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();
                log.info("카카오 로그아웃 성공: id={}", body != null ? body.get("id") : "unknown");
            }
        } catch (RestClientException e) {
            log.warn("카카오 로그아웃 API 호출 실패: {}", e.getMessage());
        }
    }
}
