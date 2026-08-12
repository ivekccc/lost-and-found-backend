package com.example.demo.report.model;


import io.swagger.v3.oas.annotations.media.Schema;
/**
 * How far back search looks.
 *
 * An enum rather than a day count in the query: ?postedWithinDays=99999 would be a valid request
 * with no meaning, and the client offers three values anyway. The number of days lives on the enum
 * value so the mapping cannot drift from the name.
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
