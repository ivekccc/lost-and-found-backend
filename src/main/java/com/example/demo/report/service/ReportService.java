package com.example.demo.report.service;

import com.example.demo.location.dto.LocationDTO;
import com.example.demo.location.dto.ReportZoneDto;
import com.example.demo.location.model.City;
import com.example.demo.location.model.DistanceBand;
import com.example.demo.location.model.Location;
import com.example.demo.location.model.Zone;
import com.example.demo.location.LocationFacade;
import com.example.demo.matching.MatchingFacade;
import com.example.demo.notification.model.NotificationType;
import com.example.demo.notification.NotificationFacade;
import com.example.demo.report.dto.CreateReportRequestDto;
import com.example.demo.report.dto.NearbyReportDTO;
import com.example.demo.report.dto.ReportDetailsDTO;
import com.example.demo.report.dto.ReportImageDTO;
import com.example.demo.report.dto.ReportImageRequestDTO;
import com.example.demo.report.dto.ReportListDTO;
import com.example.demo.report.event.ReportCreatedEvent;
import com.example.demo.report.model.AbuseReportStatus;
import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportCategory;
import com.example.demo.report.model.ReportImage;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.model.ReportType;
import com.example.demo.report.model.SavedReport;
import com.example.demo.report.model.TimeWindow;
import com.example.demo.report.repository.AbuseReportRepository;
import com.example.demo.report.repository.ReportCategoryRepository;
import com.example.demo.report.repository.ReportImageRepository;
import com.example.demo.report.repository.ReportRepository;
import com.example.demo.report.repository.ReportSpecifications;
import com.example.demo.report.repository.SavedReportRepository;
import com.example.demo.shared.exception.AccountRestrictedException;
import com.example.demo.shared.exception.InvalidChallengeException;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.shared.media.CloudinaryService;
import com.example.demo.shared.util.GeoUtils;
import com.example.demo.user.model.User;
import com.example.demo.user.model.UserStatus;
import com.example.demo.user.UserFacade;
import com.example.demo.verification.model.Claim;
import com.example.demo.verification.model.ClaimStatus;

import com.example.demo.verification.VerificationFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
@Service
@RequiredArgsConstructor
public class ReportService {

    // A listing is publicly marked "under review" only after enough distinct users report it,
    // so a single (possibly malicious) report can't taint someone else's listing.
    private static final long REPORT_VISIBILITY_THRESHOLD = 5;

    private static final int NEARBY_RESULT_LIMIT = 20;

    private static final double NEARBY_MAX_RADIUS_KM = 50;

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

   
    private static final int MAX_PAGE_SIZE = 50;

    private final ReportRepository reportRepository;
    private final ReportCategoryRepository reportCategoryRepository;
    private final UserFacade userFacade;
    private final LocationFacade locationFacade;
    
    private final AbuseReportRepository abuseReportRepository;
    private final MatchingFacade matchingFacade;
    private final VerificationFacade verificationFacade;
    private final ReportImageRepository reportImageRepository;
    private final CloudinaryService cloudinaryService;
    private final SavedReportRepository savedReportRepository;
    private final NotificationFacade notificationFacade;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.reports.default-ttl-days:60}")
    private int defaultTtlDays;

    @Transactional
    public ReportDetailsDTO createReport(CreateReportRequestDto createReportRequestDto, String userEmail) {
        User user = userFacade.requireByEmail(userEmail);

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
        report.setExpiresAt(LocalDateTime.now().plusDays(defaultTtlDays));

        Location location = locationFacade.findOrCreate(createReportRequestDto.getLocation());
        requireLocationInActiveCity(location, user);
        report.setLocation(location);

        if (createReportRequestDto.getType() == ReportType.LOST
                && createReportRequestDto.getQuestions() != null
                && !createReportRequestDto.getQuestions().isEmpty()) {
            throw new InvalidChallengeException(
                    "Lost reports cannot have verification questions — finders create them via a challenge");
        }

        Report saved = reportRepository.save(report);

        if (createReportRequestDto.getType() == ReportType.FOUND) {
            verificationFacade.createChallengeForListing(saved, user, createReportRequestDto.getQuestions());
        }

        if (createReportRequestDto.getImages() != null) {
            // A publicId arriving from the client is a reference to someone else's file: account
            // deletion wipes Cloudinary images by exactly this field. The prefix check is the real
            // defence; the uniqueness check covers images uploaded before prefixes existed.
            for (ReportImageRequestDTO imgDto : createReportRequestDto.getImages()) {
                if (imgDto.getPublicId() == null) {
                    continue;
                }
                if (!cloudinaryService.isOwnedBy(imgDto.getPublicId(), user.getId())) {
                    throw new IllegalArgumentException("Image was not uploaded by you");
                }
                if (reportImageRepository.existsByPublicId(imgDto.getPublicId())) {
                    throw new IllegalArgumentException("Image is already attached to another report");
                }
            }

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

        return toDetailsDTO(saved, user, false);
    }

    /**
     * Brings a closed listing back among the active ones.
     *
     * MATCHED used to be a dead end — out of search, out of matching, never expiring, with no way
     * back. The expiry date is pushed out too, or the sweep would kill the listing within the hour.
     */
    @Transactional
    public ReportDetailsDTO reopenReport(Long id, String userEmail) {
        User user = userFacade.requireByEmail(userEmail);

        Report report = ownedReport(id, user);

        if (report.getStatus() != ReportStatus.MATCHED
                && report.getStatus() != ReportStatus.RESOLVED
                && report.getStatus() != ReportStatus.EXPIRED) {
            throw new IllegalArgumentException(
                    "Only a closed, reunited or expired report can be reopened");
        }

        report.setStatus(ReportStatus.ACTIVE);
        report.setExpiresAt(LocalDateTime.now().plusDays(defaultTtlDays));
        // The reunion mark is cleared: a listing back in search plainly was not reunited, and a
        // stale resolvedAt would keep counting it as returned forever.
        report.setResolvedAt(null);
        reportRepository.save(report);

        return toDetailsDTO(report, user, false);
    }

    /**
     * The owner confirms the item found its way back. This is the only path into RESOLVED, and the
     * only thing the community statistics count as a success.
     *
     * Separate from closing on purpose: closing merely puts a listing away and must not inflate
     * the figure with people who simply gave up.
     */
    @Transactional
    public ReportDetailsDTO resolveReport(Long id, String userEmail) {
        User user = userFacade.requireByEmail(userEmail);

        Report report = ownedReport(id, user);

        if (report.getStatus() != ReportStatus.ACTIVE
                && report.getStatus() != ReportStatus.MATCHED
                && report.getStatus() != ReportStatus.EXPIRED) {
            throw new IllegalArgumentException(
                    "Only an active, closed or expired report can be marked reunited");
        }

        report.setStatus(ReportStatus.RESOLVED);
        report.setResolvedAt(LocalDateTime.now());
        reportRepository.save(report);

        // Same reason as closing: approveClaim requires ACTIVE, so a pending claim would hang forever.
        verificationFacade.declinePendingClaimsForListing(report.getId());
        notifyResolved(report);

        return toDetailsDTO(report, user, false);
    }

    /**
     * Tells the one person the outcome concerns who is not the owner: whoever had a claim approved.
     * No approved claim means no notification — silence beats a message to nobody.
     */
    private void notifyResolved(Report report) {
        for (Claim claim : verificationFacade.findClaimsOnReport(report.getId(), ClaimStatus.APPROVED)) {
            notificationFacade.notify(
                    claim.getClaimant().getId(),
                    NotificationType.REPORT_RESOLVED,
                    "Reunited",
                    "\"" + report.getTitle() + "\" was marked as reunited with its owner. Thanks for your help.",
                    "{\"reportId\":" + report.getId() + "}");
        }
    }

    /**
     * The owner puts their own listing away without deleting it. Exists as the counterpart to
     * reopen, since approving a claim no longer changes a listing's status.
     */
    @Transactional
    public ReportDetailsDTO closeReport(Long id, String userEmail) {
        User user = userFacade.requireByEmail(userEmail);

        Report report = ownedReport(id, user);

        if (report.getStatus() != ReportStatus.ACTIVE) {
            throw new IllegalArgumentException("Only an active report can be closed");
        }

        report.setStatus(ReportStatus.MATCHED);
        reportRepository.save(report);

        // A closed listing accepts no more claim decisions, so anything still pending would hang
        // forever while its claimant kept reading "Pending review".
        verificationFacade.declinePendingClaimsForListing(report.getId());

        return toDetailsDTO(report, user, false);
    }

    /**
     * Soft delete. The row survives because challenges and claims point at it without cascading,
     * and because the verification history belongs to the other party as much as to the owner.
     * Images stay on Cloudinary — account deletion clears those, and a hard delete here would make
     * the soft one irreversible.
     */
    @Transactional
    public void deleteReport(Long id, String userEmail) {
        User user = userFacade.requireByEmail(userEmail);

        Report report = ownedReport(id, user);

        if (report.getStatus() == ReportStatus.DELETED) {
            throw new IllegalArgumentException("Report is already deleted");
        }

        report.setStatus(ReportStatus.DELETED);
        reportRepository.save(report);

        // Same reason as closing: a deleted listing accepts no claim decisions.
        verificationFacade.declinePendingClaimsForListing(report.getId());
    }

    /**
     * The caller's own listing, or 404. Someone else's is indistinguishable from a missing one, so
     * ids cannot be probed.
     */
    private Report ownedReport(Long id, User user) {
        // Locked, not merely read: closing decides on pending claims, so it must serialise with
        // approveClaim and declineClaim, which decide on the same resource.
        return reportRepository.findByIdForUpdate(id)
                .filter(report -> report.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }


    /**
     * Paginated because the unpaginated version measured 13.9 MB and 2 s over 50k listings, while
     * the SQL itself took 36 ms. The cost was hydration and serialisation, not the database.
     */
    @Transactional(readOnly = true)
    public Page<ReportListDTO> getReports(ReportType type, Long categoryId, TimeWindow postedWithin,
                                          String search, Long zoneId, int page, int size,
                                          String userEmail) {
        User currentUser = userFacade.requireByEmail(userEmail);

        Specification<Report> spec = Specification.allOf(
                ReportSpecifications.hasStatus(ReportStatus.ACTIVE),
                ReportSpecifications.userIdNotEquals(currentUser.getId()),
                ReportSpecifications.inCity(currentUser.getActiveCity().getId()),
                ReportSpecifications.hasType(type),
                ReportSpecifications.hasCategory(categoryId),
                ReportSpecifications.postedWithin(postedWithin),
                ReportSpecifications.textContains(search),
                ReportSpecifications.inZone(zoneId),
                ReportSpecifications.withLocationZone()
        );

        // Newest first. Without an explicit sort the order was whatever the database happened to
        // return, which across pages would repeat and skip listings.
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                NEWEST_FIRST);

        Page<Report> reports = reportRepository.findAll(spec, pageable);
        Set<Long> reportedIds = findReportedIds(reports.getContent());
        return reports.map(report -> toListDTO(report, currentUser, reportedIds, null));
    }

    @Transactional(readOnly = true)
    public List<NearbyReportDTO> getNearbyReports(double latitude, double longitude,
                                                  double radiusKm, String userEmail) {
        User currentUser = userFacade.requireByEmail(userEmail);

        // Coordinates are validated, not just clamped: NaN parses fine in Spring, reaches PostGIS,
        // and turns the radius filter false for everything — an empty list with no explanation.
        if (!isValidCoordinate(latitude, -90, 90) || !isValidCoordinate(longitude, -180, 180)) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90, longitude between -180 and 180");
        }

        double effectiveRadiusKm = Math.min(Math.max(radiusKm, 0), NEARBY_MAX_RADIUS_KM);

        Specification<Report> spec = Specification.allOf(
                ReportSpecifications.hasStatus(ReportStatus.ACTIVE),
                ReportSpecifications.userIdNotEquals(currentUser.getId()),
                ReportSpecifications.inCity(currentUser.getActiveCity().getId()),
                ReportSpecifications.hasType(ReportType.FOUND),
                ReportSpecifications.withLocationZone()
        );

        List<Report> reports = reportRepository.findAll(spec);
        Set<Long> reportedIds = findReportedIds(reports);

        // Privacy by design: the caller picks the probe point freely, so an exact distance would
        // trilaterate a listing within ~100 m over a few calls. Distances are therefore measured to
        // zone centroids, and the caller's own zone is always included — otherwise someone in Borca
        // with a 5 km radius gets nothing, the centroid of Palilula being ten kilometres away.
        Zone callerZone = locationFacade
                .resolveZone(BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude))
                .orElse(null);
        Long callerZoneId = callerZone == null ? null : callerZone.getId();
        Long callerParentId = callerZone == null ? null : callerZone.getParentId();

        record ReportDistance(Report report, boolean sameZone, Double distanceKm) {}

        return reports.stream()
                .filter(report -> report.getLocation() != null && report.getLocation().getZone() != null)
                .map(report -> {
                    Zone zone = report.getLocation().getZone();
                    boolean sameZone = isSameArea(zone, callerZoneId, callerParentId);
                    double centroidDistanceKm = GeoUtils.haversineKm(
                            latitude, longitude,
                            zone.getCentroidLatitude().doubleValue(),
                            zone.getCentroidLongitude().doubleValue());
                    // No distance for your own zone: the centroid would mislead, since the listing
                    // may be on the same street.
                    return new ReportDistance(report, sameZone,
                            sameZone ? null : centroidDistanceKm);
                })
                .filter(rd -> rd.sameZone() || rd.distanceKm() <= effectiveRadiusKm)
                .sorted(Comparator
                        .comparing((ReportDistance rd) -> !rd.sameZone())
                        .thenComparing(rd -> rd.distanceKm() == null ? 0.0 : rd.distanceKm())
                        .thenComparing(rd -> rd.report().getCreatedAt(), Comparator.reverseOrder())
                        .thenComparing(rd -> rd.report().getId()))
                .limit(NEARBY_RESULT_LIMIT)
                .map(rd -> toNearbyDTO(rd.report(), currentUser, reportedIds,
                        rd.distanceKm() == null ? null : DistanceBand.of(rd.distanceKm())))
                .toList();
    }

    /**
     * Whether listing and caller share an area, honouring the hierarchy. Without the parent/child
     * check a listing on Ada Ciganlija — a hole in OSM coverage, so it resolves to the municipality
     * — would look distant to someone standing on it.
     */
    private boolean isValidCoordinate(double value, double min, double max) {
        return Double.isFinite(value) && value >= min && value <= max;
    }

    private boolean isSameArea(Zone zone, Long callerZoneId, Long callerParentId) {
        if (callerZoneId == null) {
            return false;
        }
        return zone.getId().equals(callerZoneId)
                || zone.getId().equals(callerParentId)
                || callerZoneId.equals(zone.getParentId());
    }

    private NearbyReportDTO toNearbyDTO(Report report, User viewer, Set<Long> reportedIds,
                                        DistanceBand distanceBand) {
        String thumbnailUrl = report.getImages().isEmpty() || hidesImagesFrom(report, viewer.getId())
                ? null
                : report.getImages().getFirst().getImageUrl();

        return new NearbyReportDTO(
                report.getId(),
                report.getTitle(),
                report.getType(),
                report.getCategory().getName(),
                report.getCategory().getImageUrl(),
                report.getStatus(),
                locationFor(report, viewer, false),
                report.getCreatedAt(),
                thumbnailUrl,
                reportedIds.contains(report.getId()),
                distanceBand
        );
    }

    @Transactional(readOnly = true)
    public List<ReportListDTO> getMyReports(String userEmail) {
        User currentUser = userFacade.requireByEmail(userEmail);

        Specification<Report> spec = Specification.allOf(
                ReportSpecifications.statusNot(ReportStatus.DELETED),
                ReportSpecifications.withLocationZone(),
                ReportSpecifications.userIdEquals(currentUser.getId())
        );

        List<Report> reports = reportRepository.findAll(spec, NEWEST_FIRST);
        Set<Long> reportedIds = findReportedIds(reports);
        Map<Long, Long> matchCounts = matchingFacade.countMatchesFor(
                reports.stream().map(Report::getId).toList());
        return reports.stream()
                .map(report -> toListDTO(report, currentUser, reportedIds,
                        matchCounts.getOrDefault(report.getId(), 0L)))
                .toList();
    }

    /**
     * Idempotent on purpose: a double tap, a retried request or two devices must not create two
     * rows nor return an error. The unique index is the last line of defence against a real race.
     */
    @Transactional
    public void saveReport(Long reportId, String userEmail) {
        User user = userFacade.requireByEmail(userEmail);
        Report report = findVisibleReport(reportId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (report.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Your own reports are already in My Reports");
        }
        if (savedReportRepository.existsByUserIdAndReportId(user.getId(), reportId)) {
            return;
        }

        SavedReport saved = new SavedReport();
        saved.setUser(user);
        saved.setReport(report);
        try {
            savedReportRepository.saveAndFlush(saved);
        } catch (DataIntegrityViolationException alreadySaved) {
        }
    }

    /** Idempotent — removing something that was never saved has the same outcome. */
    @Transactional
    public void unsaveReport(Long reportId, String userEmail) {
        User user = userFacade.requireByEmail(userEmail);
        savedReportRepository.deleteByUserIdAndReportId(user.getId(), reportId);
    }

    /**
     * Deliberately NOT scoped to the city. A saved listing is the user's own, like My Reports and
     * the inbox; switching cities must not hide something they set aside.
     */
    @Transactional(readOnly = true)
    public List<ReportListDTO> getSavedReports(String userEmail) {
        User currentUser = userFacade.requireByEmail(userEmail);

        List<Report> reports = savedReportRepository.findSavedReports(currentUser.getId());
        Set<Long> reportedIds = findReportedIds(reports);
        return reports.stream()
                .map(report -> toListDTO(report, currentUser, reportedIds, null))
                .toList();
    }

    /**
     * A listing the caller may see: a deleted one exists for nobody, one hidden by moderation only
     * for its owner. The rule lives here alone so the two callers cannot drift apart.
     */
    private Optional<Report> findVisibleReport(Long reportId, User viewer) {
        return reportRepository.findById(reportId)
                .filter(report -> report.getStatus() != ReportStatus.DELETED)
                .filter(report -> report.getStatus() != ReportStatus.FLAGGED
                        || report.getUser().getId().equals(viewer.getId()));
    }

    private Set<Long> findReportedIds(List<Report> reports) {
        if (reports.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = reports.stream().map(Report::getId).toList();
        return new HashSet<>(abuseReportRepository.findReportIdsWithAtLeast(
                ids, AbuseReportStatus.PENDING, REPORT_VISIBILITY_THRESHOLD));
    }

    @Transactional(readOnly = true)
    public Optional<ReportDetailsDTO> getReportById(Long id, String userEmail) {
        User currentUser = userFacade.requireByEmail(userEmail);

        return findVisibleReport(id, currentUser)
                .map(report -> toDetailsDTO(report, currentUser,
                        abuseReportRepository.countByTargetReportIdAndStatus(report.getId(), AbuseReportStatus.PENDING)
                                >= REPORT_VISIBILITY_THRESHOLD));
    }


    /**
     * Defence in depth — address search is already limited to the city. Without it, bypassing
     * autocomplete would create a listing its owner sees in My Reports and nobody sees anywhere
     * else, since it would fall out of every city filter.
     */
    private void requireLocationInActiveCity(Location location, User user) {
        Zone zone = location.getZone();
        if (zone == null) {
            throw new IllegalArgumentException(
                    "This address is outside the area the app covers");
        }
        City activeCity = user.getActiveCity();
        if (!zone.getCityId().equals(activeCity.getId())) {
            throw new IllegalArgumentException(
                    "This address is not in " + activeCity.getName()
                            + ". Switch city first, or pick an address in " + activeCity.getName() + ".");
        }
    }

    private boolean hidesImagesFrom(Report report, Long viewerId) {
        return report.getType() == ReportType.FOUND
                && !report.getUser().getId().equals(viewerId);
    }

    /**
     * Exact coordinates go only to the owner and to whoever had ownership verified. Everyone else
     * gets the zone name. Moderation reads /admin/reports instead.
     */
    private LocationDTO locationFor(Report report, User viewer, boolean revealedByVerification) {
        boolean exact = revealedByVerification || report.getUser().getId().equals(viewer.getId());

        return exact
                ? LocationDTO.fromEntity(report.getLocation())
                : LocationDTO.zonalFromEntity(report.getLocation());
    }

    private ReportListDTO toListDTO(Report report, User viewer, Set<Long> reportedIds, Long matchCount) {
        String thumbnailUrl = report.getImages().isEmpty() || hidesImagesFrom(report, viewer.getId())
                ? null
                : report.getImages().getFirst().getImageUrl();

        return new ReportListDTO(
                report.getId(),
                report.getTitle(),
                report.getType(),
                report.getCategory().getName(),
                report.getCategory().getImageUrl(),
                report.getStatus(),
                locationFor(report, viewer, false),
                report.getCreatedAt(),
                thumbnailUrl,
                reportedIds.contains(report.getId()),
                matchCount
        );
    }

    private ReportDetailsDTO toDetailsDTO(Report report, User viewer, boolean reported) {
        List<ReportImageDTO> imageDtos = hidesImagesFrom(report, viewer.getId())
                ? List.of()
                : report.getImages().stream()
                        .map(img -> new ReportImageDTO(img.getId(), img.getImageUrl(), img.getDisplayOrder()))
                        .toList();

        Long challengeId = report.getType() == ReportType.FOUND
                ? verificationFacade.findChallengeIdBy(report.getId(), report.getUser().getId())
                        .orElse(null)
                : null;

        // The challenge THIS viewer opened as a finder on someone else's lost listing. Without it the
        // client offers the action twice and only the POST reveals the mistake.
        Long myChallengeId = report.getType() == ReportType.LOST
                ? verificationFacade.findChallengeIdBy(report.getId(), viewer.getId())
                        .orElse(null)
                : null;

        // The viewer's own claim on this listing's challenge, so the claim button is not offered twice.
        Claim latestClaim = challengeId == null
                ? null
                : verificationFacade.findLatestClaim(challengeId, viewer.getId()).orElse(null);

        // Successful verification reveals the exact location here, the same way it reveals contact
        // details on an approved claim.
        boolean isOwner = report.getUser().getId().equals(viewer.getId());
        boolean revealedByVerification = !isOwner
                && verificationFacade.isContactRevealedTo(report.getId(), viewer.getId());

        return new ReportDetailsDTO(
                report.getId(),
                report.getTitle(),
                report.getDescription(),
                report.getType(),
                report.getCategory().getId(),
                report.getCategory().getName(),
                report.getCategory().getImageUrl(),
                report.getStatus(),
                locationFor(report, viewer, revealedByVerification),
                report.getCreatedAt(),
                report.getExpiresAt(),
                report.getUser().getId(),
                buildFullName(report.getUser()),
                hasText(report.getContactEmail()),
                hasText(report.getContactPhone()),
                imageDtos,
                challengeId,
                myChallengeId,
                latestClaim == null ? null : latestClaim.getId(),
                latestClaim == null ? null : latestClaim.getStatus(),
                (int) (challengeId == null ? 0 : verificationFacade.countClaimsBy(challengeId, viewer.getId())),
                verificationFacade.maxClaimAttemptsPerChallenge(),
                reported,
                describeZoneOf(report).orElse(null),
                savedReportRepository.existsByUserIdAndReportId(viewer.getId(), report.getId())
        );
    }

    private Optional<ReportZoneDto> describeZoneOf(Report report) {
        Zone zone = report.getLocation() == null ? null : report.getLocation().getZone();
        return locationFacade.describeZone(zone == null ? null : zone.getId());
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
