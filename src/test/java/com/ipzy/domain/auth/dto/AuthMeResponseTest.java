package com.ipzy.domain.auth.dto;

import com.ipzy.global.common.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthMeResponseTest {

    @Test
    @DisplayName("CustomUserPrincipal로부터 AuthMeResponse 생성")
    void fromCustomUserPrincipal() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);

        CustomUserPrincipal principal = CustomUserPrincipal.builder()
                .userId(1L)
                .email("test@example.com")
                .userName("테스트")
                .profileImageUrl("https://example.com/profile.jpg")
                .role(UserRole.USER)
                .attributes(attributes)
                .build();

        AuthMeResponse response = AuthMeResponse.from(principal);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.name()).isEqualTo("테스트");
        assertThat(response.profileImageUrl()).isEqualTo("https://example.com/profile.jpg");
    }

    @Test
    @DisplayName("프로필 이미지가 없는 CustomUserPrincipal로부터 AuthMeResponse 생성")
    void fromCustomUserPrincipalWithoutProfileImage() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);

        CustomUserPrincipal principal = CustomUserPrincipal.builder()
                .userId(2L)
                .email("noimage@example.com")
                .userName("이미지없음")
                .profileImageUrl(null)
                .role(UserRole.USER)
                .attributes(attributes)
                .build();

        AuthMeResponse response = AuthMeResponse.from(principal);

        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.email()).isEqualTo("noimage@example.com");
        assertThat(response.name()).isEqualTo("이미지없음");
        assertThat(response.profileImageUrl()).isNull();
    }
}
