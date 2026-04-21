package com.example.demo.repository;

import com.example.demo.model.Report;
import com.example.demo.model.ReportStatus;
import com.example.demo.model.ReportType;
import org.springframework.data.jpa.domain.Specification;

public final class ReportSpecifications {

    private ReportSpecifications() {
    }

    public static Specification<Report> hasType(ReportType type) {
        if (type == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("type"), type);
    }

    public static Specification<Report> statusNot(ReportStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.notEqual(root.get("status"), status);
    }

    public static Specification<Report> titleContains(String search) {
        if (search == null || search.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, builder) -> builder.like(builder.lower(root.get("title")), pattern);
    }
}
