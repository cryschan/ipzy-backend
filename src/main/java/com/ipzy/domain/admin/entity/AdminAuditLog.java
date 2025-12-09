package com.ipzy.domain.admin.entity;

import com.ipzy.domain.user.entity.User;
import com.ipzy._global.common.enums.AdminAction;
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
@Table(name = "admin_audit_logs", indexes = {
        @Index(name = "idx_audit_admin_id", columnList = "admin_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_target_type", columnList = "target_type"),
        @Index(name = "idx_audit_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdminAction action;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Type(JsonType.class)
    @Column(name = "before_data", columnDefinition = "jsonb")
    private Map<String, Object> beforeData = new HashMap<>();

    @Type(JsonType.class)
    @Column(name = "after_data", columnDefinition = "jsonb")
    private Map<String, Object> afterData = new HashMap<>();

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AdminAuditLog(User admin, AdminAction action, String targetType, Long targetId,
                         Map<String, Object> beforeData, Map<String, Object> afterData,
                         String ipAddress, String reason) {
        this.admin = admin;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.beforeData = beforeData != null ? beforeData : new HashMap<>();
        this.afterData = afterData != null ? afterData : new HashMap<>();
        this.ipAddress = ipAddress;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }
}
