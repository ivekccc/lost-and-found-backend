package com.example.demo.report.service;

import com.example.demo.location.dto.LocationDTO;
import com.example.demo.report.dto.SharedReportView;
import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportImage;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.model.ReportType;
import com.example.demo.report.repository.ReportRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportShareService {

    private static final String SHARE_PATH = "/o/";
    private static final String APP_SCHEME = "lostandfoundnative";
    private static final Set<ReportStatus> UNAVAILABLE_STATUSES =
            Set.of(ReportStatus.DELETED, ReportStatus.FLAGGED);
    private static final Set<ReportStatus> INACTIVE_STATUSES =
            Set.of(ReportStatus.MATCHED, ReportStatus.CLOSED, ReportStatus.EXPIRED);

    private final ReportRepository reportRepository;

    @Value("${app.share.public-base-url:}")
    private String publicBaseUrl;

    @Value("${app.share.android-package}")
    private String androidPackage;

    @Value("${app.share.play-store-url}")
    private String playStoreUrl;

    public String shareUrlOf(Report report) {
        return baseUrl() + SHARE_PATH + report.getShareToken();
    }

    @Transactional(readOnly = true)
    public Optional<SharedReportView> describe(String token) {
        UUID shareToken;
        try {
            shareToken = UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        return reportRepository.findByShareToken(shareToken)
                .filter(report -> !UNAVAILABLE_STATUSES.contains(report.getStatus()))
                .map(this::toView);
    }

    public String playStoreUrl() {
        return playStoreUrl;
    }

    private SharedReportView toView(Report report) {
        Locale locale = LocaleContextHolder.getLocale();
        boolean lost = report.getType() == ReportType.LOST;

        return new SharedReportView(
                stateOf(report.getStatus()),
                lost,
                report.getTitle(),
                report.getDescription(),
                report.getCategory().localizedName(locale),
                report.getLocation() == null ? null : LocationDTO.zoneLabel(report.getLocation()),
                report.getCreatedAt().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                        .withLocale(displayLocale(locale))),
                report.getRewardAmount() == null
                        ? null
                        : NumberFormat.getIntegerInstance(displayLocale(locale)).format(report.getRewardAmount()),
                imageOf(report),
                shareUrlOf(report),
                appLinkOf(report),
                playStoreUrl);
    }

    private String stateOf(ReportStatus status) {
        if (status == ReportStatus.RESOLVED) {
            return SharedReportView.STATE_RESOLVED;
        }
        return INACTIVE_STATUSES.contains(status)
                ? SharedReportView.STATE_INACTIVE
                : SharedReportView.STATE_ACTIVE;
    }

    private String imageOf(Report report) {
        if (report.getType() == ReportType.LOST && !report.getImages().isEmpty()) {
            return report.getImages().stream()
                    .map(ReportImage::getImageUrl)
                    .findFirst()
                    .orElse(null);
        }
        return report.getCategory().getImageUrl();
    }

    private String appLinkOf(Report report) {
        return "intent://report/" + report.getId()
                + "#Intent;scheme=" + APP_SCHEME
                + ";package=" + androidPackage
                + ";S.browser_fallback_url=" + URLEncoder.encode(playStoreUrl, StandardCharsets.UTF_8)
                + ";end";
    }

    private Locale displayLocale(Locale locale) {
        return "sr".equals(locale.getLanguage()) ? Locale.forLanguageTag("sr-Latn") : Locale.ENGLISH;
    }

    private String baseUrl() {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.replaceAll("/+$", "");
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
