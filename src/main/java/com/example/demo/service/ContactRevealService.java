package com.example.demo.service;

import com.example.demo.dto.ReportContactDTO;
import com.example.demo.exception.RateLimitExceededException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.ContactReveal;
import com.example.demo.model.Report;
import com.example.demo.model.ReportStatus;
import com.example.demo.model.User;
import com.example.demo.repository.ContactRevealRepository;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ContactRevealService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ContactRevealRepository contactRevealRepository;

    @Value("${app.contact-reveal.limit-per-hour}")
    private long limitPerHour;

    @Value("${app.contact-reveal.limit-per-day}")
    private long limitPerDay;

    @Transactional
    public ReportContactDTO revealContact(Long reportId, String userEmail, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Report report = reportRepository.findById(reportId)
                .filter(r -> r.getStatus() != ReportStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (!hasText(report.getContactEmail()) && !hasText(report.getContactPhone())) {
            throw new ResourceNotFoundException("No contact information available for this report");
        }

        enforceRateLimit(user.getId());

        ContactReveal reveal = new ContactReveal();
        reveal.setReport(report);
        reveal.setUser(user);
        reveal.setIpAddress(ipAddress);
        reveal.setUserAgent(userAgent);
        contactRevealRepository.save(reveal);

        return new ReportContactDTO(report.getContactEmail(), report.getContactPhone());
    }

    private void enforceRateLimit(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        long hourlyCount = contactRevealRepository.countByUserIdSince(userId, now.minusHours(1));
        if (hourlyCount >= limitPerHour) {
            throw new RateLimitExceededException(
                    "You've viewed contact details " + limitPerHour + " times in the last hour. For privacy reasons, please take a short break and try again later.",
                    3600
            );
        }

        long dailyCount = contactRevealRepository.countByUserIdSince(userId, now.minusDays(1));
        if (dailyCount >= limitPerDay) {
            throw new RateLimitExceededException(
                    "You've reached today's limit of " + limitPerDay + " contact views. This helps protect our users from spam — please try again tomorrow.",
                    86400
            );
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
