package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.event.ReportCreatedEvent;
import com.example.demo.exception.AccountRestrictedException;
import com.example.demo.exception.InvalidChallengeException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.AbuseReportRepository;
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportService {

    // A listing is publicly marked "under review" only after enough distinct users report it,
    // so a single (possibly malicious) report can't taint someone else's listing.
    private static final long REPORT_VISIBILITY_THRESHOLD = 5;

    private final ReportRepository reportRepository;
    private final ReportCategoryRepository reportCategoryRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final LocationService locationService;
    private final ChallengeService challengeService;
    private final ChallengeRepository challengeRepository;
    private final AbuseReportRepository abuseReportRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReportDetailsDTO createReport(CreateReportRequestDto createReportRequestDto, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (createReportRequestDto.getType() == ReportType.FOUND && user.getStatus() == UserStatus.PARTIALLY_BLOCKED) {
            throw new AccountRestrictedException("Your account is restricted and cannot post found items");
        }

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

        return toDetailsDTO(saved, user.getId(), false);
    }


    public List<ReportListDTO> getReports(ReportType type, String search, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Specification<Report> spec = Specification.allOf(
                ReportSpecifications.statusNot(ReportStatus.DELETED),
                ReportSpecifications.statusNot(ReportStatus.FLAGGED),
                ReportSpecifications.userIdNotEquals(currentUser.getId()),
                ReportSpecifications.hasType(type),
                ReportSpecifications.titleContains(search)
        );

        List<Report> reports = reportRepository.findAll(spec);
        Set<Long> reportedIds = findReportedIds(reports);
        return reports.stream()
                .map(report -> toListDTO(report, currentUser.getId(), reportedIds))
                .toList();
    }

    public List<ReportListDTO> getMyReports(String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Specification<Report> spec = Specification.allOf(
                ReportSpecifications.statusNot(ReportStatus.DELETED),
                ReportSpecifications.userIdEquals(currentUser.getId())
        );

        List<Report> reports = reportRepository.findAll(spec);
        Set<Long> reportedIds = findReportedIds(reports);
        return reports.stream()
                .map(report -> toListDTO(report, currentUser.getId(), reportedIds))
                .toList();
    }

    private Set<Long> findReportedIds(List<Report> reports) {
        if (reports.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = reports.stream().map(Report::getId).toList();
        return new HashSet<>(abuseReportRepository.findReportIdsWithAtLeast(
                ids, AbuseReportStatus.PENDING, REPORT_VISIBILITY_THRESHOLD));
    }

    public Optional<ReportDetailsDTO> getReportById(Long id, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return reportRepository.findById(id)
                .filter(report -> report.getStatus() != ReportStatus.DELETED)
                .filter(report -> report.getStatus() != ReportStatus.FLAGGED
                        || report.getUser().getId().equals(currentUser.getId()))
                .map(report -> toDetailsDTO(report, currentUser.getId(),
                        abuseReportRepository.countByTargetReportIdAndStatus(report.getId(), AbuseReportStatus.PENDING)
                                >= REPORT_VISIBILITY_THRESHOLD));
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

    private ReportListDTO toListDTO(Report report, Long viewerId, Set<Long> reportedIds) {
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
                thumbnailUrl,
                reportedIds.contains(report.getId())
        );
    }

    private ReportDetailsDTO toDetailsDTO(Report report, Long viewerId, boolean reported) {
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
                challengeId,
                reported
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
