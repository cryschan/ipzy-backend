package com.ipzy.domain.user.entity;

import com.ipzy.global.common.BaseEntity;
import com.ipzy.global.common.enums.UserRole;
import com.ipzy.global.common.enums.UserStatus;
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

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> preferences = new HashMap<>();

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public User(String email, String password, String name, String phone,
                String profileImageUrl, UserRole role, UserStatus status,
                Map<String, Object> preferences) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
        this.role = role != null ? role : UserRole.USER;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.preferences = preferences != null ? preferences : new HashMap<>();
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateProfile(String name, String phone, String profileImageUrl) {
        this.name = name;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
    }

    public void updatePreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void delete() {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.deletedAt = null;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return this.status == UserStatus.DELETED;
    }
}
