package com.ipzy.domain.user.entity;

import com.ipzy._global.common.BaseEntity;
import com.ipzy._global.common.enums.UserRole;
import com.ipzy._global.common.enums.UserStatus;
import com.ipzy.domain.user.vo.UserStylePreference;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 엔티티 - OAuth2 소셜 로그인 사용자 정보
 */
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"provider", "provider_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /**
     * 관리자 비밀번호 (BCrypt 암호화)
     * 일반 사용자는 OAuth2 로그인만 사용하므로 null
     * ADMIN/SUPER_ADMIN 역할의 사용자만 비밀번호 설정
     */
    @Column(length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> preferences = new HashMap<>();

    @Type(JsonType.class)
    @Column(name = "style_preference", columnDefinition = "jsonb")
    private UserStylePreference stylePreference;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public User(String email, String name, String phone, String provider,
                String providerId, String profileImageUrl, String password,
                UserRole role, UserStatus status, Map<String, Object> preferences) {
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.provider = provider;
        this.providerId = providerId;
        this.profileImageUrl = profileImageUrl;
        this.password = password;
        this.role = role != null ? role : UserRole.USER;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.preferences = preferences != null ? preferences : new HashMap<>();
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateOAuthInfo(String name, String profileImageUrl) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    /**
     * 다른 소셜 계정을 연동합니다.
     * 기존 이메일로 가입된 사용자가 다른 소셜로 로그인할 때 사용됩니다.
     *
     * @param provider   새로운 OAuth2 Provider (KAKAO, NAVER, GOOGLE)
     * @param providerId Provider에서 발급한 사용자 ID
     */
    public void linkSocialAccount(String provider, String providerId) {
        this.provider = provider;
        this.providerId = providerId;
    }

    public void updateProfile(String name, String phone, String profileImageUrl) {
        this.name = name;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
    }

    public void updatePreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }

    public void updateStylePreference(UserStylePreference stylePreference) {
        this.stylePreference = stylePreference;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void delete() {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 계정 탈퇴를 수행합니다.
     *
     * <p>상태를 DELETED로 변경하고, 개인정보를 마스킹 처리합니다.
     * 법적 보존 의무가 있는 데이터(Payment, Subscription)는 연관 엔티티에서 별도 처리합니다.
     */
    public void withdraw() {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();

        // 개인정보 마스킹
        this.email = "deleted_" + this.id + "_" + System.currentTimeMillis() + "@ipzy.com";
        this.name = "탈퇴한 사용자";
        this.phone = null;
        this.profileImageUrl = null;
        this.providerId = "deleted_" + this.id;
        this.preferences = null;
        this.stylePreference = null;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.deletedAt = null;
    }

    /**
     * 관리자에 의해 회원 상태를 변경합니다.
     * @param newStatus 변경할 상태
     */
    public void changeStatus(UserStatus newStatus) {
        if (newStatus == UserStatus.DELETED) {
            this.deletedAt = LocalDateTime.now();
        } else if (this.status == UserStatus.DELETED && newStatus != UserStatus.DELETED) {
            this.deletedAt = null;
        }
        this.status = newStatus;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return this.status == UserStatus.DELETED;
    }

    /**
     * 관리자 비밀번호를 변경합니다.
     * @param encodedPassword BCrypt로 암호화된 비밀번호
     */
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /**
     * 관리자 여부를 확인합니다.
     */
    public boolean isAdmin() {
        return this.role == UserRole.ADMIN || this.role == UserRole.SUPER_ADMIN;
    }
}
