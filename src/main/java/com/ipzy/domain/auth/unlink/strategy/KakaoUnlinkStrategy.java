package com.ipzy.domain.auth.unlink.strategy;

import com.ipzy.domain.auth.unlink.OAuth2UnlinkStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * 카카오 연결 끊기 전략.
 *
 * <p>API: POST https://kapi.kakao.com/v1/user/unlink
 * <p>인증: Authorization: Bearer {access_token}
 *
 * <p>로그아웃(/v1/user/logout)과 달리, unlink는 앱과의 연결을 완전히 해제합니다.
 * 재로그인 시 동의 화면이 다시 표시됩니다.
 */
@Slf4j
@Component
public class KakaoUnlinkStrategy implements OAuth2UnlinkStrategy {

    private static final String UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";
    private final RestClient restClient;

    public KakaoUnlinkStrategy() {
        this.restClient = RestClient.create();
    }

    @Override
    public String getProvider() {
        return "KAKAO";
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean unlink(String accessToken) {
        try {
            Map<String, Object> response = restClient.post()
                    .uri(UNLINK_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("id")) {
                log.info("카카오 연결 끊기 성공: id={}", response.get("id"));
                return true;
            }

            log.warn("카카오 연결 끊기 응답에 id가 없습니다: {}", response);
            return false;

        } catch (RestClientException e) {
            log.warn("카카오 연결 끊기 API 호출 실패: {}", e.getMessage());
            return false;
        }
    }
}
