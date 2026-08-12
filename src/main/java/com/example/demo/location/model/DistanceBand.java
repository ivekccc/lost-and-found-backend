package com.example.demo.location.model;

import io.swagger.v3.oas.annotations.media.Schema;
/**
 * A distance band instead of a number of kilometres.
 *
 * All distances in public responses are measured to zone CENTROIDS, so this is not a privacy
 * measure — the zone name already gives the centroid away. It is about honesty: "0.3 km" on a
 * zone of roughly 1 km² is an artefact accurate to about half a kilometre.
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
