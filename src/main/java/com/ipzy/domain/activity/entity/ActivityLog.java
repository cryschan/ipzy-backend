package com.ipzy.domain.activity.entity;

import com.ipzy.domain.user.entity.User;
import com.ipzy.global.common.enums.ActivityType;
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
@Table(name = "activity_logs", indexes = {
        @Index(name = "idx_activity_user_id", columnList = "user_id"),
        @Index(name = "idx_activity_type", columnList = "activity_type"),
        @Index(name = "idx_activity_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private ActivityType activityType;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ActivityLog(User user, ActivityType activityType, Map<String, Object> metadata,
                       String ipAddress, String userAgent) {
        this.user = user;
        this.activityType = activityType;
        this.metadata = metadata != null ? metadata : new HashMap<>();
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = LocalDateTime.now();
    }
}
