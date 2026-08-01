package com.example.demo.repository;

import com.example.demo.model.Report;
import com.example.demo.model.ReportStatus;
import com.example.demo.model.ReportType;
import com.example.demo.model.TimeWindow;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class ReportSpecifications {

    private ReportSpecifications() {
    }

    public static Specification<Report> hasType(ReportType type) {
        if (type == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("type"), type);
    }

    public static Specification<Report> hasCategory(Long categoryId) {
        if (categoryId == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("category").get("id"), categoryId);
    }

    /**
     * Oglasi objavljeni unutar datog prozora.
     *
     * Granica se racuna u trenutku poziva, ne jednom pri pokretanju: „poslednja 24 h" mora da
     * znaci 24 h od SADA. Posledica koju vredi znati: isti filter posle ponoci vraca drugaciji
     * skup, pa rezultat nije stabilan kroz vreme.
     */
    public static Specification<Report> postedWithin(TimeWindow window) {
        if (window == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.greaterThanOrEqualTo(
                root.get("createdAt"), LocalDateTime.now().minusDays(window.getDays()));
    }

    public static Specification<Report> statusNot(ReportStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.notEqual(root.get("status"), status);
    }

    public static Specification<Report> hasStatus(ReportStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    public static Specification<Report> userIdEquals(Long userId) {
        if (userId == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Report> userIdNotEquals(Long userId) {
        if (userId == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.notEqual(root.get("user").get("id"), userId);
    }

    /**
     * Oglas pripada gradu koji korisnik trenutno pretrazuje.
     *
     * Put je report -> location -> zone -> city, sa INNER join-ovima namerno: oglas bez
     * lokacije ili bez razresene zone nema grad, pa ispada iz pretrage sam od sebe. To je
     * i cilj — takav oglas ionako ne ulazi u matching niti se vidi u "Found nearby", pa bi
     * se u pretrazi pojavljivao kao mrtav unos. Od C12 nadalje ga vise nije ni moguce
     * napraviti (lokacija je obavezna), ovo pokriva redove nastale ranije.
     */
    public static Specification<Report> inCity(Long cityId) {
        if (cityId == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(
                root.join("location").join("zone").get("cityId"), cityId);
    }

    /**
     * Ucitava lokaciju i njenu zonu zajedno sa oglasom, jednim upitom.
     *
     * Bez ovoga je svaka lista oglasa N+1: {@code Location.zone} je LAZY, a labela lokacije
     * ({@code LocationDTO.zonalFromEntity}) i ime grada u admin listi citaju zonu za SVAKI red.
     * Na nepaginiranoj listi to je jedan upit po oglasu.
     *
     * {@code getResultType() != Long.class} preskace fetch u count upitu: Hibernate odbija
     * fetch join u upitu koji vraca skalar. Danas nijedan pozivalac ne koristi {@code Page},
     * ali kad se paginacija doda (stavka D17), ovo je razlika izmedu radi i puca.
     */
    public static Specification<Report> withLocationZone() {
        return (root, query, builder) -> {
            if (query != null && query.getResultType() != Long.class) {
                root.fetch("location", JoinType.LEFT).fetch("zone", JoinType.LEFT);
            }
            return builder.conjunction();
        };
    }

    /**
     * Pojam se trazi u naslovu ILI u opisu.
     *
     * Ranije se zvala {@code titleContains} i gledala samo naslov, pa „crni kozni novcanik"
     * nije nalazilo oglas naslovljen „Izgubljen novcanik" sa tim opisom — a opis je mesto na
     * kom stoje boja, marka i sve po cemu se stvar zaista prepoznaje.
     *
     * Namerno {@code LIKE}, a ne {@code pg_trgm} similarity (koju matching engine koristi):
     * „sadrzi" je predvidivo i radi na delu reci, dok similarity uvodi prag koji se podesava
     * i menja znacenje pretrage iz „nadji" u „rangiraj".
     *
     * Ostaje ograniceno: {@code lower()} ne izjednacava dijakritike, pa „novcanik" i dalje ne
     * nalazi „novcanik" sa kvacicom. Za to treba {@code unaccent} prosirenje.
     */
    public static Specification<Report> textContains(String search) {
        if (search == null || search.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("title")), pattern),
                builder.like(builder.lower(root.get("description")), pattern));
    }
}
