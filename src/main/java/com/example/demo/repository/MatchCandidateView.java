package com.example.demo.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface MatchCandidateView {

    Long getId();

    LocalDateTime getCreatedAt();

    BigDecimal getLatitude();

    BigDecimal getLongitude();

    Double getSimilarity();
}
