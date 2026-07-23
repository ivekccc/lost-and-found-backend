package com.example.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.matching")
@Data
public class MatchingProperties {

    private double maxDistanceKm = 25;

    private int weightDistance = 40;

    private int weightText = 35;

    private int weightTime = 25;

    private int timeDecayDays = 30;

    private int scoreThreshold = 40;
}
