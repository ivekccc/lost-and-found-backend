package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.event.ReportCreatedEvent;
import com.example.demo.exception.InvalidChallengeException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.ChallengeRepository;
import com.example.demo.repository.LocationRepository;
import com.example.demo.repository.ReportCategoryRepository;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.ReportSpecifications;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final ReportCategoryRepository reportCategoryRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final LocationService locationService;
    private final ChallengeService challengeService;
    private final ChallengeRepository challengeRepository;
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

        if (createReportRequestDto.getType() == ReportType.LOST
                && createReportRequestDto.getQuestions() != null
                && !createReportRequestDto.getQuestions().isEmpty()) {
            throw new InvalidChallengeException(
                    "Lost reports cannot have verification questions — finders create them via a challenge");
        }

        Report saved = reportRepository.save(report);

        if (createReportRequestDto.getType() == ReportType.FOUND) {
            challengeService.createChallenge(saved, user, createReportRequestDto.getQuestions());
        }

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

        return toDetailsDTO(saved, user.getId());
    }


    public List<ReportListDTO> getReports(ReportType type, String search, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Specification<Report> spec = Specification.allOf(
                ReportSpecifications.statusNot(ReportStatus.DELETED),
                ReportSpecifications.userIdNotEquals(currentUser.getId()),
                ReportSpecifications.hasType(type),
                ReportSpecifications.titleContains(search)
        );

        return reportRepository.findAll(spec).stream()
                .map(report -> toListDTO(report, currentUser.getId()))
                .toList();
    }

    public List<ReportListDTO> getMyReports(String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Specification<Report> spec = Specification.allOf(
                ReportSpecifications.statusNot(ReportStatus.DELETED),
                ReportSpecifications.userIdEquals(currentUser.getId())
        );

        return reportRepository.findAll(spec).stream()
                .map(report -> toListDTO(report, currentUser.getId()))
                .toList();
    }

    public Optional<ReportDetailsDTO> getReportById(Long id, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return reportRepository.findById(id)
                .filter(report -> report.getStatus() != ReportStatus.DELETED)
                .map(report -> toDetailsDTO(report, currentUser.getId()));
    }

    private Location findOrCreateLocation(LocationRequestDTO dto) {
        Optional<Location> existing = locationRepository.findByOsmId(dto.getOsmId());
        if (existing.isPresent()) {
            return existing.get();
        }

        Location location = locationService.lookupLocation(dto.getOsmId(), dto.getOsmType());
        return locationRepository.save(location);
    }

    private boolean hidesImagesFrom(Report report, Long viewerId) {
        return report.getType() == ReportType.FOUND
                && !report.getUser().getId().equals(viewerId);
    }

    private ReportListDTO toListDTO(Report report, Long viewerId) {
        String thumbnailUrl = report.getImages().isEmpty() || hidesImagesFrom(report, viewerId)
                ? null
                : report.getImages().getFirst().getImageUrl();

        return new ReportListDTO(
                report.getId(),
                report.getTitle(),
                report.getType(),
                report.getCategory().getName(),
                report.getCategory().getImageUrl(),
                report.getStatus(),
                LocationDTO.fromEntity(report.getLocation()),
                report.getCreatedAt(),
                thumbnailUrl
        );
    }

    private ReportDetailsDTO toDetailsDTO(Report report, Long viewerId) {
        List<ReportImageDTO> imageDtos = hidesImagesFrom(report, viewerId)
                ? List.of()
                : report.getImages().stream()
                        .map(img -> new ReportImageDTO(img.getId(), img.getImageUrl(), img.getDisplayOrder()))
                        .toList();

        Long challengeId = report.getType() == ReportType.FOUND
                ? challengeRepository.findByReportIdAndAuthorId(report.getId(), report.getUser().getId())
                        .map(Challenge::getId)
                        .orElse(null)
                : null;

        return new ReportDetailsDTO(
                report.getId(),
                report.getTitle(),
                report.getDescription(),
                report.getType(),
                report.getCategory().getId(),
                report.getCategory().getName(),
                report.getCategory().getImageUrl(),
                report.getStatus(),
                LocationDTO.fromEntity(report.getLocation()),
                report.getCreatedAt(),
                report.getExpiresAt(),
                report.getUser().getId(),
                buildFullName(report.getUser()),
                hasText(report.getContactEmail()),
                hasText(report.getContactPhone()),
                imageDtos,
                challengeId
        );
    }

    private String buildFullName(User user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String combined = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
        String trimmed = combined.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
