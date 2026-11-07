package com.example.demo.shared.stats;

import java.time.LocalDate;

public record DailyCount(LocalDate day, long count) {
}
