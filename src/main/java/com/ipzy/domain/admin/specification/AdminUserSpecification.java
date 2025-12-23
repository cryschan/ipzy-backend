package com.ipzy.domain.admin.specification;

import com.ipzy._global.common.enums.UserRole;
import com.ipzy._global.common.enums.UserStatus;
import com.ipzy.domain.user.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * 관리자용 회원 검색 Specification
 * JPA Criteria API를 사용한 동적 쿼리 생성
 */
public class AdminUserSpecification {

    private AdminUserSpecification() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * 이메일 또는 이름에 키워드 포함 여부
     */
    public static Specification<User> emailOrNameContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("name")), pattern)
            );
        };
    }

    /**
     * 회원 상태 일치 여부
     */
    public static Specification<User> statusEquals(UserStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    /**
     * 역할 일치 여부
     */
    public static Specification<User> roleEquals(UserRole role) {
        return (root, query, cb) -> {
            if (role == null) {
                return null;
            }
            return cb.equal(root.get("role"), role);
        };
    }

    /**
     * 가입일 범위 조건
     */
    public static Specification<User> createdAtBetween(LocalDate from, LocalDate to) {
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
                return cb.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        from.atStartOfDay()
                );
            }
            return cb.lessThan(
                    root.get("createdAt"),
                    to.plusDays(1).atStartOfDay()
            );
        };
    }

    /**
     * 삭제되지 않은 회원만 조회
     */
    public static Specification<User> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }
}
