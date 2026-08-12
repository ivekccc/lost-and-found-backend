package com.example.demo.report.repository;

import com.example.demo.location.model.Zone;
import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.model.ReportType;
import com.example.demo.report.model.TimeWindow;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;
public final class ReportSpecifications {

    private ReportSpecifications() {
    }

    public static Specification<Report> hasType(ReportType type) {
        if (type == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("type"), type);
    }

    public static Specification<Report> hasCategory(Long categoryId) {
        if (categoryId == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("category").get("id"), categoryId);
    }

    /**
     * The cut-off is computed per call, not once at startup: "last 24 h" must mean 24 h from now.
     */
    public static Specification<Report> postedWithin(TimeWindow window) {
        if (window == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.greaterThanOrEqualTo(
                root.get("createdAt"), LocalDateTime.now().minusDays(window.getDays()));
    }

    public static Specification<Report> statusNot(ReportStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.notEqual(root.get("status"), status);
    }

    public static Specification<Report> hasStatus(ReportStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    public static Specification<Report> userIdEquals(Long userId) {
        if (userId == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Report> userIdNotEquals(Long userId) {
        if (userId == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.notEqual(root.get("user").get("id"), userId);
    }

    /**
     * Path is report -> location -> zone -> city, with INNER joins on purpose: a listing without a
     * resolved zone has no city and drops out of search by itself, which is the intent.
     */
    public static Specification<Report> inCity(Long cityId) {
        if (cityId == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(
                root.join("location").join("zone").get("cityId"), cityId);
    }

    /**
     * Matches the given zone OR any of its children. The parent branch carries the feature: a
     * listing resolves to the DEEPEST zone covering it, so filtering by municipality would
     * otherwise miss almost everything.
     */
    public static Specification<Report> inZone(Long zoneId) {
        if (zoneId == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> {
            Path<Zone> zone = root.join("location").get("zone");
            return builder.or(
                    builder.equal(zone.get("id"), zoneId),
                    builder.equal(zone.get("parentId"), zoneId));
        };
    }

    /**
     * Loads location and zone with the listing. Without it every list is N+1, since the location
     * label reads the zone per row. The count query is skipped: Hibernate rejects a fetch join in
     * a query returning a scalar.
     */
    public static Specification<Report> withLocationZone() {
        return (root, query, builder) -> {
            if (query != null && query.getResultType() != Long.class) {
                root.fetch("location", JoinType.LEFT).fetch("zone", JoinType.LEFT);
            }
            return builder.conjunction();
        };
    }

    /**
     * Searches title OR description — the description is where colour, brand and the details that
     * actually identify an item are written.
     *
     * LIKE rather than pg_trgm similarity: "contains" is predictable and works on part of a word,
     * while similarity introduces a threshold and turns search into ranking. Diacritics are folded
     * by the database on both sides — Java's Normalizer would not touch "đ", a separate character.
     */
    public static Specification<Report> textContains(String search) {
        if (search == null || search.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, builder) -> {
            Expression<String> foldedPattern =
                    unaccented(builder, builder.literal(pattern));
            return builder.or(
                    builder.like(
                            unaccented(builder, builder.lower(root.get("title"))),
                            foldedPattern),
                    builder.like(
                            unaccented(builder, builder.lower(root.get("description"))),
                            foldedPattern));
        };
    }

    private static Expression<String> unaccented(CriteriaBuilder builder,
                                                 Expression<String> value) {
        return builder.function("unaccent", String.class, value);
    }
}
