package com.ipzy.domain.auth.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthMeResponseTest {

    @Test
    @DisplayName("OAuth2User로부터 AuthMeResponse 생성")
    void fromOAuth2User() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);
        attributes.put("userId", 1L);
        attributes.put("email", "test@example.com");
        attributes.put("name", "테스트");

        OAuth2User oAuth2User = new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                "id"
        );

        AuthMeResponse response = AuthMeResponse.from(oAuth2User);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.name()).isEqualTo("테스트");
    }

    @Test
    @DisplayName("null 값이 포함된 OAuth2User로부터 AuthMeResponse 생성")
    void fromOAuth2UserWithNullValues() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);
        attributes.put("userId", null);
        attributes.put("email", null);
        attributes.put("name", null);

        OAuth2User oAuth2User = new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                "id"
        );

        AuthMeResponse response = AuthMeResponse.from(oAuth2User);

        assertThat(response.userId()).isNull();
        assertThat(response.email()).isNull();
        assertThat(response.name()).isNull();
    }
}
