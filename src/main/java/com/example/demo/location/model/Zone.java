package com.example.demo.location.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * Administrative zone used for the public, aggregated view of a listing's location.
 *
 * Two levels: municipalities and, below them, local communities or settlements. The layer is
 * density-adaptive — a dense core gets small cells, a sparse periphery large ones — so the number
 * of people per zone stays more even than a uniform grid would give.
 *
 * The boundary geometry is deliberately NOT mapped. It is only ever used from SQL, so the project
 * needs neither hibernate-spatial nor JTS; adding the field without them breaks startup.
 */
@Entity
@Table(name = "zones")
@Data
@NoArgsConstructor
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "level", nullable = false)
    private Short level;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "parent_name", length = 100)
    private String parentName;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    /** Plain column rather than @ManyToOne City: @Data would drag a lazy proxy into toString(). */
    @Column(name = "city_id", nullable = false)
    private Long cityId;

    @Column(name = "centroid_latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal centroidLatitude;

    @Column(name = "centroid_longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal centroidLongitude;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
