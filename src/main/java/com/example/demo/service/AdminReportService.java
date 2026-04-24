package com.example.demo.service;

import com.example.demo.dto.AdminReportDetailsDTO;
import com.example.demo.dto.LocationDTO;
import com.example.demo.dto.ReportImageDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Report;
import com.example.demo.model.ReportStatus;
import com.example.demo.model.User;
import com.example.demo.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final ReportRepository reportRepository;

    public AdminReportDetailsDTO getReportById(Long id) {
        Report report = reportRepository.findById(id)
                .filter(r -> r.getStatus() != ReportStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        return mapToDetailsDTO(report);
    }

    private AdminReportDetailsDTO mapToDetailsDTO(Report report) {
        List<ReportImageDTO> imageDtos = report.getImages().stream()
                .map(img -> new ReportImageDTO(img.getId(), img.getImageUrl(), img.getDisplayOrder()))
                .toList();

        return new AdminReportDetailsDTO(
                report.getId(),
                report.getTitle(),
                report.getDescription(),
                report.getType(),
                report.getCategory().getId(),
                report.getCategory().getName(),
                report.getStatus(),
                LocationDTO.fromEntity(report.getLocation()),
                report.getCreatedAt(),
                report.getExpiresAt(),
                report.getUser().getId(),
                buildFullName(report.getUser()),
                report.getContactEmail(),
                report.getContactPhone(),
                imageDtos
        );
    }

    private String buildFullName(User user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String combined = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
        String trimmed = combined.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
