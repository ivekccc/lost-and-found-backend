package com.example.demo.user.repository;

import com.example.demo.user.model.User;
import com.example.demo.user.model.UserRole;
import com.example.demo.user.model.UserStatus;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> hasRole(UserRole role) {
        if (role == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("role"), role);
    }

    public static Specification<User> hasStatus(UserStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    public static Specification<User> matches(String search) {
        if (search == null || search.isBlank()) {
            return Specification.unrestricted();
        }
        String trimmed = search.trim();
        String term = trimmed.toLowerCase();
        String digits = trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
        Long id = digits.matches("\\d{1,18}") ? Long.valueOf(digits) : null;
        return (root, query, builder) -> {
            Expression<String> literal = builder.literal(term);
            Predicate text = builder.or(
                    contains(builder, builder.lower(root.get("email")), literal),
                    contains(builder, builder.lower(root.get("username")), literal),
                    contains(builder, unaccented(builder, builder.lower(fullName(root, builder))), unaccented(builder, literal)));
            return id == null ? text : builder.or(builder.equal(root.get("id"), id), text);
        };
    }

    private static Predicate contains(CriteriaBuilder builder, Expression<String> haystack, Expression<String> needle) {
        return builder.greaterThan(builder.function("strpos", Integer.class, haystack, needle), 0);
    }

    private static Expression<String> fullName(Root<User> root, CriteriaBuilder builder) {
        return builder.concat(
                builder.concat(builder.coalesce(root.get("firstName"), ""), " "),
                builder.coalesce(root.get("lastName"), ""));
    }

    private static Expression<String> unaccented(CriteriaBuilder builder, Expression<String> value) {
        return builder.function("unaccent", String.class, value);
    }
}
