package com.ipzy.domain.admin.specification;

import com.ipzy._global.common.enums.SubscriptionStatus;
import com.ipzy.domain.subscription.entity.Subscription;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class AdminSubscriptionSpecification {

    private AdminSubscriptionSpecification() {
    }

    public static Specification<Subscription> userEmailOrNameContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            Join<?, ?> user = root.join("user");
            return cb.or(
                cb.like(cb.lower(user.get("email")), pattern),
                cb.like(cb.lower(user.get("name")), pattern)
            );
        };
    }

    public static Specification<Subscription> statusEquals(SubscriptionStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Subscription> planNameEquals(String planName) {
        return (root, query, cb) -> {
            if (planName == null || planName.isBlank()) {
                return null;
            }
            Join<?, ?> plan = root.join("plan");
            return cb.equal(plan.get("name"), planName);
        };
    }

    public static Specification<Subscription> createdAtBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from != null && to != null) {
                return cb.between(
                    root.get("createdAt"),
                    from.atStartOfDay(),
                    to.plusDays(1).atStartOfDay()
                );
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay());
            }
            return cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay());
        };
    }
}
