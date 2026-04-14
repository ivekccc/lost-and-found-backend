package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.event.ReportCreatedEvent;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.LocationRepository;
import com.example.demo.repository.ReportCategoryRepository;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final LocationRepository locationRepository;
    private final LocationService locationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReportDetailsDTO createReport(CreateReportRequestDto createReportRequestDto, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ReportCategory reportCategory = reportCategoryRepository.findById(createReportRequestDto.getCategoryId()).orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Report report = new Report();
        report.setTitle(createReportRequestDto.getTitle());
        report.setDescription(createReportRequestDto.getDescription());
        report.setType(createReportRequestDto.getType());
        report.setCategory(reportCategory);
        report.setContactEmail(createReportRequestDto.getContactEmail());
        report.setContactPhone(createReportRequestDto.getContactPhone());
        report.setUser(user);
        report.setStatus(ReportStatus.ACTIVE);

        if (createReportRequestDto.getLocation() != null) {
            Location location = findOrCreateLocation(createReportRequestDto.getLocation());
            report.setLocation(location);
        }

        Report saved = reportRepository.save(report);


        if (createReportRequestDto.getImages() != null) {
            for (int i = 0; i < createReportRequestDto.getImages().size(); i++) {
                ReportImageRequestDTO imgDto = createReportRequestDto.getImages().get(i);
                ReportImage image = new ReportImage();
                image.setReport(saved);
                image.setImageUrl(imgDto.getImageUrl());
                image.setPublicId(imgDto.getPublicId());
                image.setDisplayOrder(i);
                saved.getImages().add(image);
            }
            reportRepository.save(saved);
        }

        eventPublisher.publishEvent(new ReportCreatedEvent(this, saved.getId(), user.getId(), saved.getTitle()));

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

    private Location findOrCreateLocation(LocationRequestDTO dto) {
        Optional<Location> existing = locationRepository.findByOsmId(dto.getOsmId());
        if (existing.isPresent()) {
            return existing.get();
        }

        Location location = locationService.lookupLocation(dto.getOsmId(), dto.getOsmType());
        return locationRepository.save(location);
    }

    private ReportListDTO toListDTO(Report report) {
        String thumbnailUrl = report.getImages().isEmpty()
                ? null
                : report.getImages().getFirst().getImageUrl();

        return new ReportListDTO(
                report.getId(),
                report.getTitle(),
                report.getType(),
                report.getCategory().getName(),
                report.getStatus(),
                LocationDTO.fromEntity(report.getLocation()),
                report.getCreatedAt(),
                thumbnailUrl
        );
    }

    private ReportDetailsDTO toDetailsDTO(Report report) {
        List<ReportImageDTO> imageDtos = report.getImages().stream()
                .map(img -> new ReportImageDTO(img.getId(), img.getImageUrl(), img.getDisplayOrder()))
                .toList();

        return new ReportDetailsDTO(
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
                report.getContactEmail(),
                report.getContactPhone(),
                imageDtos
        );
    }
}
