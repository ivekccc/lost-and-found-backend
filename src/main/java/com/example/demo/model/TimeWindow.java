package com.example.demo.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Koliko unazad pretraga gleda.
 *
 * Oglasi zive 60 dana, pa se stariji gomilaju. Za izgubljenu stvar to nije svejedno:
 * novcanik izgubljen juce i onaj od pre 50 dana su dve razlicite stvari, a sortiranje po
 * datumu ih samo poreda — ne odseca.
 *
 * Namerno enum, a ne broj dana u parametru: {@code ?postedWithinDays=99999} bi bio validan
 * zahtev bez znacenja, a klijent svejedno nudi samo tri vrednosti. Isti oblik kao
 * {@link DistanceBand} — nekoliko imenovanih opsega, labele na klijentu.
 *
 * Broj dana stoji NA vrednosti enuma, da mapiranje ne bi zivelo u {@code switch}-u negde
 * dalje i razislo se od imena.
 */
@Schema(name = "TimeWindow", enumAsRef = true,
        description = "How far back the search looks, counted from the moment of the request.")
public enum TimeWindow {
    LAST_24_HOURS(1),
    LAST_3_DAYS(3),
    LAST_WEEK(7);

    private final int days;

    TimeWindow(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }
}
