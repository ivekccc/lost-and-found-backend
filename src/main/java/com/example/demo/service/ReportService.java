package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.event.ReportCreatedEvent;
import com.example.demo.exception.AccountRestrictedException;
import com.example.demo.exception.InvalidChallengeException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.AbuseReportRepository;
import com.example.demo.repository.ChallengeRepository;
import com.example.demo.repository.ClaimRepository;
import com.example.demo.repository.LocationRepository;
import com.example.demo.repository.ReportCategoryRepository;
import com.example.demo.repository.ReportImageRepository;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.ReportSpecifications;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
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

    private final ReportRepository reportRepository;
    private final ReportCategoryRepository reportCategoryRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final LocationService locationService;
    private final ChallengeService challengeService;
    private final ChallengeRepository challengeRepository;
    private final AbuseReportRepository abuseReportRepository;
    private final ReportMatchService reportMatchService;
    private final ClaimRepository claimRepository;
    private final ClaimService claimService;
    private final ReportImageRepository reportImageRepository;
    private final ZoneService zoneService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.reports.default-ttl-days:60}")
    private int defaultTtlDays;

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
        report.setExpiresAt(LocalDateTime.now().plusDays(defaultTtlDays));

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
            // publicId stize od klijenta i ranije se upisivao bez ikakve provere. Posto se
            // brisanje naloga oslanja bas na to polje da bi uklonilo slike sa Cloudinary-ja,
            // napadac je mogao da procita publicId sa tudje slike (vidi se iz imageUrl-a),
            // prijavi ga kao svoj na novom oglasu, pa obrise svoj nalog — i time trajno
            // unisti fotografiju na tudjem, zivom oglasu. Isti publicId zato sme da postoji
            // samo jednom u bazi.
            for (ReportImageRequestDTO imgDto : createReportRequestDto.getImages()) {
                if (imgDto.getPublicId() != null
                        && reportImageRepository.existsByPublicId(imgDto.getPublicId())) {
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
     * Vraca zatvoren oglas medju aktivne.
     *
     * Do sada je MATCHED bio slepa ulica: oglas ispada iz pretrage i iz matchinga, ne prima
     * vise challenge, nikad ne istice, a svi njegovi meceve postaju nevidljivi OBEMA stranama.
     * Kod izgubljenog oglasa u to stanje ga je mogao gurnuti NALAZAC odobravanjem claim-a, pa
     * je vlasnik ostajao bez svog oglasa i bez ijednog nacina da ga vrati.
     *
     * expiresAt se pomera na pun TTL bezuslovno. Bez toga bi oglas koji je u MATCHED stajao
     * duze od svojih 60 dana sledeci sweep (na sat vremena) odmah ugasio i poslao korisniku
     * "Your report expired" — sekund posle sto ga je vratio.
     *
     * Postojeci report_matches redovi se ne brisu kad oglas ode u MATCHED, samo se filtriraju
     * u citanju, pa se posle vracanja pojave odmah i bez preracunavanja.
     */
    @Transactional
    public ReportDetailsDTO reopenReport(Long id, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Report report = ownedReport(id, user);

        if (report.getStatus() != ReportStatus.MATCHED) {
            throw new IllegalArgumentException("Only a closed report can be reopened");
        }

        report.setStatus(ReportStatus.ACTIVE);
        report.setExpiresAt(LocalDateTime.now().plusDays(defaultTtlDays));
        reportRepository.save(report);

        return toDetailsDTO(report, user, false);
    }

    /**
     * Vlasnik sam sklanja svoj oglas iz pretrage i matchinga, bez brisanja.
     *
     * Postoji kao par reopen-u: posto odobravanje claim-a na izgubljenom oglasu vise ne menja
     * status (vidi ClaimService.approveClaim), vlasnik je jedini ko taj oglas moze da zatvori
     * posto je predmet vracen.
     */
    @Transactional
    public ReportDetailsDTO closeReport(Long id, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Report report = ownedReport(id, user);

        if (report.getStatus() != ReportStatus.ACTIVE) {
            throw new IllegalArgumentException("Only an active report can be closed");
        }

        report.setStatus(ReportStatus.MATCHED);
        reportRepository.save(report);

        // Zatvoren oglas vise ne prima odluke o claim-ovima (approveClaim trazi ACTIVE), pa bi
        // svaki claim koji ceka ostao PENDING zauvek i podnosilac bi bez objasnjenja gledao
        // "Pending review". Zato se odbijaju odmah, uz notifikaciju.
        claimService.declinePendingClaimsForReport(report.getId());

        return toDetailsDTO(report, user, false);
    }

    /**
     * Oglas u vlasnistvu pozivaoca, ili 404. Tudji oglas se NE razlikuje od nepostojeceg —
     * ista konvencija kao ReportMatchService.getMatchesForReport, da id-jevi ne cure.
     */
    private Report ownedReport(Long id, User user) {
        // Zakljucava se, ne samo cita: zatvaranje oglasa odbija claim-ove koji cekaju odluku,
        // pa je to odluka o istom resursu kao approveClaim i declineClaim i mora da se sa
        // njima serijalizuje. Bez toga vlasnik koji zatvara oglas u istom trenutku kad
        // nalazac odobrava daje istu trku koju zakljucavanje u ClaimService resava.
        return reportRepository.findByIdForUpdate(id)
                .filter(report -> report.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }


    @Transactional(readOnly = true)
    public List<ReportListDTO> getReports(ReportType type, String search, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Specification<Report> spec = Specification.allOf(
                ReportSpecifications.hasStatus(ReportStatus.ACTIVE),
                ReportSpecifications.userIdNotEquals(currentUser.getId()),
                ReportSpecifications.hasType(type),
                ReportSpecifications.titleContains(search)
        );

        List<Report> reports = reportRepository.findAll(spec);
        Set<Long> reportedIds = findReportedIds(reports);
        return reports.stream()
                .map(report -> toListDTO(report, currentUser, reportedIds, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NearbyReportDTO> getNearbyReports(double latitude, double longitude,
                                                  double radiusKm, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Radijus se klampuje, ali su koordinate do sada isle sirove. Vrednost van opsega ili
        // NaN (koji Spring uredno parsira) stize do PostGIS-a, a haversine vrati NaN pa filter
        // po radijusu uvek ispadne netacan — korisnik dobije praznu listu bez objasnjenja,
        // ili 500 iz baze. Bolje je odbiti odmah.
        if (!isValidCoordinate(latitude, -90, 90) || !isValidCoordinate(longitude, -180, 180)) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90, longitude between -180 and 180");
        }

        double effectiveRadiusKm = Math.min(Math.max(radiusKm, 0), NEARBY_MAX_RADIUS_KM);

        Specification<Report> spec = Specification.allOf(
                ReportSpecifications.hasStatus(ReportStatus.ACTIVE),
                ReportSpecifications.userIdNotEquals(currentUser.getId()),
                ReportSpecifications.hasType(ReportType.FOUND)
        );

        List<Report> reports = reportRepository.findAll(spec);
        Set<Long> reportedIds = findReportedIds(reports);

        // Privacy by design: nikada se ne poredi tacna lokacija oglasa sa probnom tackom —
        // pozivalac probnu tacku slobodno bira, pa bi mu tacna distanca kroz nekoliko
        // poziva trilateracijom odala lokaciju (~100 m). Umesto toga: uvek se ukljucuje
        // ZONA POZIVAOCA (inace bi korisnik u Borci sa radijusom 5 km dobio praznu listu,
        // jer je centroid Palilule desetak km daleko), a susedne zone ulaze ako im je
        // centroid u radijusu. Oglasi bez poznate zone se ne prikazuju (isto kao na mapi).
        Zone callerZone = zoneService
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
                    // Za sopstvenu zonu distanca se ne prikazuje: rastojanje do centroida
                    // bi bilo obmanjujuce (oglas moze biti u istoj ulici, a centroid daleko).
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
     * Da li oglas i pozivalac pripadaju istom podrucju, uzimajuci u obzir hijerarhiju.
     *
     * Bez provere roditelj/dete nastaje ovakav propust: oglas na Adi Ciganliji nema mesnu
     * zajednicu (rupa u OSM podacima), pa se razresava na opstinu Cukarica, cija je
     * reprezentativna tacka nekoliko kilometara dalje. Korisnik 300 m odatle razresava se
     * u mesnu zajednicu Cukaricka padina, zone se ne poklapaju, distanca do centroida
     * Cukarice je ~6 km i sa podrazumevanih 5 km oglas 300 m daleko ispada iz liste.
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
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Specification<Report> spec = Specification.allOf(
                ReportSpecifications.statusNot(ReportStatus.DELETED),
                ReportSpecifications.userIdEquals(currentUser.getId())
        );

        List<Report> reports = reportRepository.findAll(spec);
        Set<Long> reportedIds = findReportedIds(reports);
        Map<Long, Long> matchCounts = reportMatchService.getMatchCounts(
                reports.stream().map(Report::getId).toList());
        return reports.stream()
                .map(report -> toListDTO(report, currentUser, reportedIds,
                        matchCounts.getOrDefault(report.getId(), 0L)))
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

    @Transactional(readOnly = true)
    public Optional<ReportDetailsDTO> getReportById(Long id, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return reportRepository.findById(id)
                .filter(report -> report.getStatus() != ReportStatus.DELETED)
                .filter(report -> report.getStatus() != ReportStatus.FLAGGED
                        || report.getUser().getId().equals(currentUser.getId()))
                .map(report -> toDetailsDTO(report, currentUser,
                        abuseReportRepository.countByTargetReportIdAndStatus(report.getId(), AbuseReportStatus.PENDING)
                                >= REPORT_VISIBILITY_THRESHOLD));
    }


    private Location findOrCreateLocation(LocationRequestDTO dto) {
        Optional<Location> existing = locationRepository.findByOsmId(dto.getOsmId());
        if (existing.isPresent()) {
            return existing.get();
        }

        Location location = locationService.lookupLocation(dto.getOsmId(), dto.getOsmType());
        location.setZone(zoneService
                .resolveZone(location.getLatitude(), location.getLongitude())
                .orElse(null));
        return locationRepository.save(location);
    }

    private boolean hidesImagesFrom(Report report, Long viewerId) {
        return report.getType() == ReportType.FOUND
                && !report.getUser().getId().equals(viewerId);
    }

    /**
     * Privacy by design: tacna lokacija se otkriva samo vlasniku oglasa i onome kome
     * je verifikacija vlasnistva odobrena. Svi ostali dobijaju samo naziv zone.
     * Moderacija ide kroz /admin/reports (uvek tacna lokacija), ne kroz granu po roli.
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
                ? challengeRepository.findByReportIdAndAuthorId(report.getId(), report.getUser().getId())
                        .map(Challenge::getId)
                        .orElse(null)
                : null;

        // Challenge koji je OVAJ posmatrac otvorio kao nalazac na tudjem izgubljenom oglasu.
        // Bez toga klijent ne zna da je vec poslao pitanja, pa nudi akciju drugi put i tek
        // POST vrati 400. Isti podatak je i jedina veza nalazaca sa claim-ovima na njegovom
        // challenge-u — vlasnik oglasa nije autor, pa mu challengeId iznad ne pomaze.
        Long myChallengeId = report.getType() == ReportType.LOST
                ? challengeRepository.findByReportIdAndAuthorId(report.getId(), viewer.getId())
                        .map(Challenge::getId)
                        .orElse(null)
                : null;

        // Stanje sopstvenog claim-a posmatraca na challenge-u ovog oglasa. Ogledalo je
        // ReportChallengeDto, koji isto vec nosi za izgubljene oglase; bez toga se dugme
        // "Claim ownership" nudi i posle poslatog claim-a.
        List<Claim> myClaims = challengeId == null
                ? List.of()
                : claimRepository.findByChallengeIdAndClaimantIdOrderBySubmittedAtDesc(
                        challengeId, viewer.getId());
        Claim latestClaim = myClaims.isEmpty() ? null : myClaims.get(0);

        // Detalj oglasa je jedina povrsina gde uspesna verifikacija otkriva tacnu
        // lokaciju — po istom principu kao otkrivanje kontakta na odobrenom claimu.
        // Upit se izvrsava samo za tudje oglase; vlasnik ionako vidi tacnu lokaciju.
        boolean isOwner = report.getUser().getId().equals(viewer.getId());
        boolean revealedByVerification = !isOwner && claimRepository.existsClaimOnReportWithStatus(
                report.getId(), viewer.getId(), ClaimStatus.APPROVED);

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
                myClaims.size(),
                ClaimService.MAX_ATTEMPTS_PER_CHALLENGE,
                reported,
                describeZoneOf(report).orElse(null)
        );
    }

    private Optional<ReportZoneDto> describeZoneOf(Report report) {
        Zone zone = report.getLocation() == null ? null : report.getLocation().getZone();
        return zoneService.describeZone(zone == null ? null : zone.getId());
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
