package com.example.demo.shared.ratelimit;

import java.time.Duration;

public enum RateLimitPolicy {
    AUTH_PER_ADDRESS(60, Duration.ofMinutes(1)),
    AUTH_CODE_PER_ADDRESS(10, Duration.ofMinutes(10)),
    AUTH_EMAIL_PER_ADDRESS(5, Duration.ofMinutes(10)),
    LOCATION_SEARCH_PER_USER(60, Duration.ofMinutes(1)),
    UPLOAD_SIGNATURE_PER_USER(40, Duration.ofHours(1)),
    REPORT_CREATION_PER_USER(15, Duration.ofDays(1)),
    TYPING_PER_USER(60, Duration.ofMinutes(1));

    private final int capacity;
    private final Duration period;

    RateLimitPolicy(int capacity, Duration period) {
        this.capacity = capacity;
        this.period = period;
    }

    public int capacity() {
        return capacity;
    }

    public Duration period() {
        return period;
    }
}
