package com.example.demo.model;

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
 * Grad koji korisnik moze da izabere kao podrucje pretrage.
 *
 * Okvir ({@code bbox*}) je unija granica zona nivoa 1 tog grada, izracunata u V47. Koristi
 * se kao {@code viewbox} za LocationIQ pretragu adresa, pa je izvedenost iz istih poligona
 * koji cine zone bitna: da okvir i zone ne mogu da opisu razlicito podrucje, odnosno da
 * korisnik ne moze da izabere adresu koja posle nema zonu.
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

    // Podrazumevano TRUE i u entitetu, ne samo u DDL-u: Hibernate ukljucuje kolonu u INSERT
    // i sa null vrednoscu, pa DEFAULT iz seme ne bi ni dosao do reci.
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
