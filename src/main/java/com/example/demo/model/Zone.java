package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Administrativna zona koja se koristi za javni, agregirani prikaz lokacije oglasa.
 *
 * Hijerarhija ima dva nivoa: {@code level = 1} je beogradska gradska opstina,
 * {@code level = 2} mesna zajednica ili naseljeno mesto. Razresavanje tacke bira
 * najdublju zonu koja je sadrzi (SQL funkcija {@code zone_resolve}), uz pad na nivo 1
 * tamo gde jedinica nivoa 2 ne postoji, pa je pokrivenost 100%.
 *
 * NAMERNO nema polje za kolonu {@code boundary geometry(MultiPolygon, 4326)} —
 * geometrija se koristi samo iz SQL-a (ST_Covers u ZoneRepository), pa projektu
 * nije potrebna hibernate-spatial/JTS zavisnost. Ne dodavati je kao polje bez te
 * zavisnosti, jer bi Hibernate pukao pri startu. Iz istog razloga {@code area_km2}
 * ostaje nemapiran — koristi ga samo SQL.
 *
 * {@code parentId} je NAMERNO obicna kolona, a ne {@code @ManyToOne Zone parent}:
 * klasa je {@code @Data}, pa bi asocijacija uvukla LAZY proxy u {@code toString()} i
 * {@code equals()} i pucala van transakcije, na primer iz log poziva.
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
