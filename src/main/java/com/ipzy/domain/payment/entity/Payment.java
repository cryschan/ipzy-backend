package com.ipzy.domain.payment.entity;

import com.ipzy.domain.subscription.entity.Subscription;
import com.ipzy.domain.user.entity.User;
import com.ipzy.global.common.BaseEntity;
import com.ipzy.global.common.enums.PaymentMethod;
import com.ipzy.global.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false, length = 10)
    private String currency = "KRW";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(length = 500)
    private String description;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    public Payment(User user, Subscription subscription, Integer amount, String currency,
                   PaymentMethod method, PaymentStatus status, String description) {
        this.user = user;
        this.subscription = subscription;
        this.amount = amount;
        this.currency = currency != null ? currency : "KRW";
        this.method = method;
        this.status = status != null ? status : PaymentStatus.PENDING;
        this.description = description;
    }

    public void complete(String transactionId, String receiptUrl) {
        this.status = PaymentStatus.COMPLETED;
        this.transactionId = transactionId;
        this.receiptUrl = receiptUrl;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public void refund() {
        this.status = PaymentStatus.REFUNDED;
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
    }

    public boolean isCompleted() {
        return this.status == PaymentStatus.COMPLETED;
    }
}
