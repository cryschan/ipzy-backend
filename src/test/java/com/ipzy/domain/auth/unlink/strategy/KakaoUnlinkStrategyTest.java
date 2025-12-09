package com.ipzy.domain.auth.unlink.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoUnlinkStrategyTest {

    private final KakaoUnlinkStrategy strategy = new KakaoUnlinkStrategy();

    @Test
    @DisplayName("Provider 이름은 KAKAO를 반환한다")
    void getProvider_returnsKakao() {
        // when
        String provider = strategy.getProvider();

        // then
        assertThat(provider).isEqualTo("KAKAO");
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 unlink 호출 시 false를 반환한다")
    void unlink_withInvalidToken_returnsFalse() {
        // given
        String invalidToken = "invalid_token";

        // when
        boolean result = strategy.unlink(invalidToken);

        // then
        assertThat(result).isFalse();
    }
}