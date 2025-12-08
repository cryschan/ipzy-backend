package com.ipzy.domain.subscription.entity;

import com.ipzy._global.common.BaseEntity;
import com.ipzy._global.common.enums.BillingPeriod;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "subscription_plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false, length = 10)
    private String currency = "KRW";

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period", length = 20)
    private BillingPeriod billingPeriod;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> features = new HashMap<>();

    @Column(length = 500)
    private String description;

    private String badge;

    @Builder
    public SubscriptionPlan(String name, String displayName, Integer price, String currency,
                            BillingPeriod billingPeriod, Map<String, Object> features,
                            String description, String badge) {
        this.name = name;
        this.displayName = displayName;
        this.price = price;
        this.currency = currency != null ? currency : "KRW";
        this.billingPeriod = billingPeriod;
        this.features = features != null ? features : new HashMap<>();
        this.description = description;
        this.badge = badge;
    }
}
