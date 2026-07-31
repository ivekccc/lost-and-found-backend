package com.example.demo.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Opseg udaljenosti umesto konkretnog broja kilometara.
 *
 * Sve udaljenosti u javnim odgovorima racunaju se do CENTROIDA zone, ne do tacne lokacije.
 * Dok je zona bila cela gradska opstina, prikaz "4.2 km" je bio bezopasno grub. Sa zonama
 * od oko 1 km2 vrednost "0.3 km" i dalje nije tacna udaljenost do predmeta — i dalje je
 * artefakt centroida, sa greskom reda pola kilometra — ali sada IZGLEDA kao precizan
 * podatak. Opseg saopstava istu upotrebljivu informaciju bez te lazne preciznosti.
 *
 * Napomena o privatnosti: ovo NIJE mera zastite. Posto se naziv zone ionako javno prikazuje,
 * napadac iz njega vec zna centroid, pa mu udaljenost do centroida ne daje nista novo.
 * Trilateracija opisana u istoriji projekta zatvorena je jos prelaskom sa tacnih na zonske
 * udaljenosti.
 */
@Schema(name = "DistanceBand", enumAsRef = true)
public enum DistanceBand {
    UNDER_1_KM,
    FROM_1_TO_2_KM,
    FROM_2_TO_5_KM,
    OVER_5_KM;

    public static DistanceBand of(double distanceKm) {
        if (distanceKm < 1.0) {
            return UNDER_1_KM;
        }
        if (distanceKm < 2.0) {
            return FROM_1_TO_2_KM;
        }
        if (distanceKm < 5.0) {
            return FROM_2_TO_5_KM;
        }
        return OVER_5_KM;
    }
}
