package com.example.demo.service;

import com.example.demo.dto.ReportDetailsDTO;
import com.example.demo.dto.ReportListDTO;
import com.example.demo.model.Report;
import com.example.demo.model.ReportStatus;
import com.example.demo.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;

    public List<ReportListDTO> getAllReports() {
        return reportRepository.findAll().stream()
                .filter(report -> report.getStatus() != ReportStatus.DELETED)
                .map(this::toListDTO)
                .collect(Collectors.toList());
    }

    public Optional<ReportDetailsDTO> getReportById(Long id) {
        return reportRepository.findById(id)
                .filter(report -> report.getStatus() != ReportStatus.DELETED)
                .map(this::toDetailsDTO);
    }

    private ReportListDTO toListDTO(Report report) {
        return new ReportListDTO(
                report.getId(),
                report.getTitle(),
                report.getType(),
                report.getCategory().getName(),
                report.getStatus(),
                report.getLocation(),
                report.getCreatedAt()
        );
    }

    private ReportDetailsDTO toDetailsDTO(Report report) {
        return new ReportDetailsDTO(
                report.getId(),
                report.getTitle(),
                report.getDescription(),
                report.getType(),
                report.getCategory().getId(),
                report.getCategory().getName(),
                report.getStatus(),
                report.getLocation(),
                report.getCreatedAt(),
                report.getExpiresAt(),
                report.getUser().getId(),
                report.getContactEmail(),
                report.getContactPhone()
        );
    }
}
