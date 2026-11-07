package com.example.demo.shared.stats;

import java.time.LocalDate;

public interface DailyCountView {

    LocalDate getDay();

    Long getCount();

    default DailyCount toDailyCount() {
        return new DailyCount(getDay(), getCount() == null ? 0 : getCount());
    }
}
