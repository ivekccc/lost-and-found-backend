package com.example.demo.report.repository;

import com.example.demo.report.model.AbuseReason;
import com.example.demo.report.model.AbuseReport;
import com.example.demo.report.model.AbuseReportStatus;
import com.example.demo.report.model.AbuseTargetType;

import org.springframework.data.jpa.domain.Specification;

public final class AbuseReportSpecifications {

    private AbuseReportSpecifications() {
    }

    public static Specification<AbuseReport> hasStatus(AbuseReportStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    public static Specification<AbuseReport> hasTargetType(AbuseTargetType targetType) {
        if (targetType == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("targetType"), targetType);
    }

    public static Specification<AbuseReport> hasReason(AbuseReason reason) {
        if (reason == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("reason"), reason);
    }

    public static Specification<AbuseReport> targetsReport(Long reportId) {
        if (reportId == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("targetReport").get("id"), reportId);
    }

    public static Specification<AbuseReport> targetsUser(Long userId) {
        if (userId == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("targetUser").get("id"), userId);
    }
}
