package com.example.demo.repository;

import com.example.demo.model.Report;
import com.example.demo.model.ReportStatus;
import com.example.demo.model.ReportType;
import com.example.demo.model.TimeWindow;
import com.example.demo.model.Zone;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
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
     * Oglas lezi u datoj zoni ILI u nekoj njenoj podzoni.
     *
     * Grana po roditelju je nosivi deo, ne sitnica: oglas se razresava na NAJDUBLJU zonu koja ga
     * pokriva, dakle po pravilu na mesnu zajednicu. Filter po opstini bi bez toga hvatao samo onu
     * manjinu oglasa koja je pala na nivo 1 (tacke koje nijedna mesna zajednica ne pokriva), pa bi
     * izgledao kao da opstina nema skoro nista.
     *
     * Isti izraz radi za oba nivoa, jer {@code parent_id} zone nivoa 2 nikad nije id druge zone
     * nivoa 2 — izbor mesne zajednice zato pogadja tacno nju.
     *
     * Koristi {@code get("zone")} nad postojecim join-om lokacije umesto novog {@code join}: put
     * report -> location vec spajaju {@code inCity} i {@code withLocationZone}.
     */
    public static Specification<Report> inZone(Long zoneId) {
        if (zoneId == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> {
            Path<Zone> zone = root.join("location").get("zone");
            return builder.or(
                    builder.equal(zone.get("id"), zoneId),
                    builder.equal(zone.get("parentId"), zoneId));
        };
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
     * Dijakritika se presavija kroz {@code unaccent} (V51), pa „novcanik" nalazi i „novčanik".
     * Presavijanje radi iskljucivo baza, i nad kolonom i nad pojmom: da se pojam presavija u
     * Javi, {@code java.text.Normalizer} bi razdvojio slova sa akcentom ali NE i „đ", koje je
     * zaseban znak (U+0111) — pa bi „djak" i „đak" i dalje bili dve razlicite stvari.
     */
    public static Specification<Report> textContains(String search) {
        if (search == null || search.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, builder) -> {
            Expression<String> foldedPattern =
                    unaccented(builder, builder.literal(pattern));
            return builder.or(
                    builder.like(
                            unaccented(builder, builder.lower(root.get("title"))),
                            foldedPattern),
                    builder.like(
                            unaccented(builder, builder.lower(root.get("description"))),
                            foldedPattern));
        };
    }

    private static Expression<String> unaccented(CriteriaBuilder builder,
                                                 Expression<String> value) {
        return builder.function("unaccent", String.class, value);
    }
}
