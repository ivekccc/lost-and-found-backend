package com.example.demo.report.dto;

public record SharedReportView(
        String state,
        boolean lost,
        String title,
        String description,
        String category,
        String place,
        String date,
        String reward,
        String imageUrl,
        String pageUrl,
        String appLink,
        String playStoreUrl) {

    public static final String STATE_ACTIVE = "active";
    public static final String STATE_RESOLVED = "resolved";
    public static final String STATE_INACTIVE = "inactive";
}
