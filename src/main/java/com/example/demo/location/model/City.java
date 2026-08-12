package com.example.demo.location.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * A city the user can pick as their search area. The bounding box is the union of that city's
 * level-1 zone boundaries, so the box and the zones cannot drift apart.
 */
@Entity
@Table(name = "cities")
@Data
@NoArgsConstructor
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 8)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "osm_relation_id", nullable = false)
    private Long osmRelationId;

    @Column(name = "center_latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal centerLatitude;

    @Column(name = "center_longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal centerLongitude;

    @Column(name = "bbox_min_latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal bboxMinLatitude;

    @Column(name = "bbox_min_longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal bboxMinLongitude;

    @Column(name = "bbox_max_latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal bboxMaxLatitude;

    @Column(name = "bbox_max_longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal bboxMaxLongitude;

    // Defaulted in the entity as well as the DDL: Hibernate includes the column in the INSERT
    // even when null, so the schema default would never apply.
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
