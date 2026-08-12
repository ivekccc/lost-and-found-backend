package com.example.demo.matching.repository;

import com.example.demo.matching.model.ReportMatch;
import com.example.demo.matching.model.ReportMatchStatus;

import org.springframework.data.jpa.domain.Specification;
public final class ReportMatchSpecifications {

    private ReportMatchSpecifications() {
    }

    public static Specification<ReportMatch> hasStatus(ReportMatchStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    public static Specification<ReportMatch> scoreAtLeast(Integer minScore) {
        if (minScore == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("score"), minScore);
    }
}
