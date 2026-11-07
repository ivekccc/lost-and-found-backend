package com.example.demo.report.repository;

import com.example.demo.location.model.Location;
import com.example.demo.location.model.Zone;
import com.example.demo.report.dto.ZoneReportCountDto;
import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
public class ReportZoneCountRepositoryImpl implements ReportZoneCountRepository {

    private final EntityManager entityManager;

    @Override
    public List<ZoneReportCountDto> countByZone(Specification<Report> specification) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = builder.createTupleQuery();
        Root<Report> report = query.from(Report.class);
        Join<Report, Location> location = report.join("location");
        Join<Location, Zone> zone = location.join("zone");

        Expression<Long> lost = builder.sum(builder.<Long>selectCase()
                .when(builder.equal(report.get("type"), ReportType.LOST), 1L)
                .otherwise(0L));
        Expression<Long> found = builder.sum(builder.<Long>selectCase()
                .when(builder.equal(report.get("type"), ReportType.FOUND), 1L)
                .otherwise(0L));

        query.multiselect(
                        zone.get("id"),
                        zone.get("name"),
                        zone.get("parentId"),
                        zone.get("parentName"),
                        zone.get("centroidLatitude"),
                        zone.get("centroidLongitude"),
                        lost,
                        found)
                .where(specification.toPredicate(report, query, builder))
                .groupBy(
                        zone.get("id"),
                        zone.get("name"),
                        zone.get("parentId"),
                        zone.get("parentName"),
                        zone.get("centroidLatitude"),
                        zone.get("centroidLongitude"));

        return entityManager.createQuery(query).getResultList().stream()
                .map(row -> new ZoneReportCountDto(
                        row.get(0, Long.class),
                        row.get(1, String.class),
                        row.get(2, Long.class),
                        row.get(3, String.class),
                        row.get(4, BigDecimal.class),
                        row.get(5, BigDecimal.class),
                        row.get(6, Long.class),
                        row.get(7, Long.class)))
                .toList();
    }
}
