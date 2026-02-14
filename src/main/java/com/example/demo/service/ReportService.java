package com.example.demo.service;

import com.example.demo.dto.CreateReportRequestDto;
import com.example.demo.dto.ReportDetailsDTO;
import com.example.demo.dto.ReportListDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.ReportCategoryRepository;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final ReportCategoryRepository reportCategoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReportDetailsDTO createReport(CreateReportRequestDto createReportRequestDto, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ReportCategory reportCategory = reportCategoryRepository.findById(createReportRequestDto.getCategoryId()).orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Report report = new Report();
        report.setTitle(createReportRequestDto.getTitle());
        report.setDescription(createReportRequestDto.getDescription());
        report.setType(createReportRequestDto.getType());
        report.setCategory(reportCategory);
        report.setLocation(createReportRequestDto.getLocation());
        report.setContactEmail(createReportRequestDto.getContactEmail());
        report.setContactPhone(createReportRequestDto.getContactPhone());
        report.setUser(user);
        report.setStatus(ReportStatus.ACTIVE);

        Report saved = reportRepository.save(report);
        return toDetailsDTO(saved);
    }



    public List<ReportListDTO> getReports(ReportType type) {
        List<Report> reports;

        if (type != null) {
            reports = reportRepository.findByTypeAndStatusNot(type, ReportStatus.DELETED);
        } else {
            reports = reportRepository.findByStatusNot(ReportStatus.DELETED);
        }

        return reports.stream()
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
