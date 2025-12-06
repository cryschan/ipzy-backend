package com.ipzy.domain.auth.dto;

import com.ipzy.domain.user.entity.User;
import com.ipzy.global.common.enums.UserRole;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Spring Security OAuth2 인증 Principal
 * - 세션에 저장되어 인증된 사용자 정보 제공
 * - Controller에서 @AuthenticationPrincipal로 주입받아 사용
 */
@Getter
public class CustomUserPrincipal implements OAuth2User, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String email;
    private final String userName;  // OAuth2User.getName()과 충돌 방지
    private final String profileImageUrl;
    private final UserRole role;
    private final Map<String, Object> attributes;

    @Builder
    public CustomUserPrincipal(Long userId, String email, String userName,
                               String profileImageUrl, UserRole role,
                               Map<String, Object> attributes) {
        this.userId = userId;
        this.email = email;
        this.userName = userName;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
        this.attributes = attributes;
    }

    /**
     * User 엔티티로부터 Principal 생성
     */
    public static CustomUserPrincipal from(User user, Map<String, Object> attributes) {
        return CustomUserPrincipal.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .userName(user.getName())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .attributes(attributes)
                .build();
    }

    // ========== OAuth2User 구현 ==========

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
