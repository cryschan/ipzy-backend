package com.ipzy.domain.admin.specification;

import com.ipzy._global.common.enums.ClothingCategory;
import com.ipzy.domain.product.entity.Product;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class AdminProductSpecification {

    private AdminProductSpecification() {
    }

    public static Specification<Product> nameOrBrandContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            Join<?, ?> brand = root.join("brand");
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(brand.get("name")), pattern),
                cb.like(cb.lower(brand.get("displayName")), pattern)
            );
        };
    }

    public static Specification<Product> categoryEquals(ClothingCategory category) {
        return (root, query, cb) -> {
            if (category == null) {
                return null;
            }
            return cb.equal(root.get("category"), category);
        };
    }

    public static Specification<Product> brandIdEquals(Long brandId) {
        return (root, query, cb) -> {
            if (brandId == null) {
                return null;
            }
            return cb.equal(root.get("brand").get("id"), brandId);
        };
    }

    public static Specification<Product> isActiveEquals(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) {
                return null;
            }
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    public static Specification<Product> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Product> createdAtBetween(LocalDate from, LocalDate to) {
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
