package com.ipzy.domain.subscription.entity;
import com.ipzy.domain.user.entity.User;
import com.ipzy._global.common.BaseEntity;
import com.ipzy._global.common.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status = SubscriptionStatus.PENDING;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = true;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Builder
    public Subscription(User user, SubscriptionPlan plan, SubscriptionStatus status,
                        LocalDateTime startDate, LocalDateTime endDate, Boolean autoRenew) {
        this.user = user;
        this.plan = plan;
        this.status = status != null ? status : SubscriptionStatus.PENDING;
        this.startDate = startDate;
        this.endDate = endDate;
        this.autoRenew = autoRenew != null ? autoRenew : true;
    }

    public void activate() {
        this.status = SubscriptionStatus.ACTIVE;
    }

    public void cancel(String reason) {
        this.status = SubscriptionStatus.CANCELLED;
        this.autoRenew = false;
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = reason;
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
    }

    public void renew(LocalDateTime newEndDate) {
        this.endDate = newEndDate;
        this.status = SubscriptionStatus.ACTIVE;
    }

    public void updatePlan(SubscriptionPlan plan, SubscriptionStatus status,
                           LocalDateTime startDate, LocalDateTime endDate,
                           boolean autoRenew) {
        this.plan = plan;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.autoRenew = autoRenew;
        this.cancelledAt = null;
        this.cancelReason = null;
    }

    public boolean isActive() {
        return this.status == SubscriptionStatus.ACTIVE &&
                LocalDateTime.now().isBefore(this.endDate);
    }

    public boolean isExpired() {
        return this.status == SubscriptionStatus.EXPIRED ||
                LocalDateTime.now().isAfter(this.endDate);
    }
}
