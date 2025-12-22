package com.ipzy.domain.admin.specification;

import com.ipzy.domain.quiz.entity.QuizSession;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class AdminQuizSessionSpecification {

    private AdminQuizSessionSpecification() {
    }

    public static Specification<QuizSession> userEmailOrNameContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            Join<?, ?> user = root.join("user", JoinType.LEFT);
            return cb.or(
                cb.like(cb.lower(user.get("email")), pattern),
                cb.like(cb.lower(user.get("name")), pattern)
            );
        };
    }

    public static Specification<QuizSession> quizIdEquals(Long quizId) {
        return (root, query, cb) -> {
            if (quizId == null) {
                return null;
            }
            return cb.equal(root.get("quiz").get("id"), quizId);
        };
    }

    public static Specification<QuizSession> completedEquals(Boolean completed) {
        return (root, query, cb) -> {
            if (completed == null) {
                return null;
            }
            return cb.equal(root.get("completed"), completed);
        };
    }

    public static Specification<QuizSession> createdAtBetween(LocalDate from, LocalDate to) {
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
