-- Dodela roditelja jedinicama nivoa 2 i provera da je sloj upotrebljiv.
--
-- Roditelj se odreduje po NAJVECOJ POVRSINI PRESEKA, ne po ST_Contains: granice mesnih
-- zajednica i granice opstina su nezavisno digitalizovane u OSM-u, pa dete redovno viri
-- nekoliko metara van roditelja i ST_Contains bi za veliki deo redova vratio false.

UPDATE zones child
SET parent_id = (
    SELECT parent.id
    FROM zones parent
    WHERE parent.level = 1
      AND parent.boundary && child.boundary
    ORDER BY ST_Area(ST_Intersection(parent.boundary, child.boundary)) DESC, parent.id
    LIMIT 1)
WHERE child.level = 2;

DO $$
DECLARE
    orphans INT;
    strays INT;
    overlapping INT;
    invalid_edge_metres NUMERIC;
BEGIN
    SELECT count(*) INTO orphans FROM zones WHERE level = 2 AND parent_id IS NULL;
    IF orphans > 0 THEN
        RAISE EXCEPTION '% zona nivoa 2 nema roditelja', orphans;
    END IF;

    -- Dete koje je manje od 80%% unutar dodeljenog roditelja znaci da je preklapanje sa
    -- vise opstina priblizno, pa dodela nije pouzdana.
    SELECT count(*) INTO strays
    FROM zones child
    JOIN zones parent ON parent.id = child.parent_id
    WHERE child.level = 2
      AND ST_Area(ST_Intersection(parent.boundary, child.boundary)) < 0.80 * ST_Area(child.boundary);
    IF strays > 0 THEN
        RAISE EXCEPTION '% zona nivoa 2 je manje od 80%% unutar svog roditelja', strays;
    END IF;

    -- Nivo 2 mora biti disjunktan: dve zone preko istog tla znace da razresavanje zone
    -- zavisi od tiebreak-a, pa ista tacka moze dobiti razlicit odgovor posle osvezavanja
    -- geometrije. Prag od 500 m2 pokriva ostatke od uproscavanja, ne pravo preklapanje.
    SELECT count(*) INTO overlapping
    FROM zones a
    JOIN zones b ON a.id < b.id
    WHERE a.level = 2 AND b.level = 2
      AND ST_Area(ST_CollectionExtract(ST_Intersection(a.boundary, b.boundary), 3)::geography) > 500;
    IF overlapping > 0 THEN
        RAISE EXCEPTION '% parova zona nivoa 2 se preklapa preko 500 m2', overlapping;
    END IF;

    -- Per-feature uproscavanje pomera svaku stranu zajednicke granice nezavisno i pravi
    -- procepe duz CELE zajednicke granice; generator zato koristi ST_CoverageSimplify.
    -- Ovde se ne trazi savrsen coverage: 271 jedinica je u OSM-u mapirana nezavisno, pa
    -- izmedu nekih seoskih naselja postoji stvarna rupa u podacima (izmereno: 4 zone,
    -- ukupno ~343 m ivice). Tacka u takvoj rupi pada na tier 2 u zone_resolve i dobija
    -- opstinu, sto je posteno. Prag od 2 km hvata regresiju na per-feature uproscavanje,
    -- koja bi dala desetine kilometara neuparenih ivica, a toleri­se zatecene rupe.
    SELECT coalesce(sum(ST_Length(bad_edge::geography)), 0) INTO invalid_edge_metres
    FROM (
        SELECT ST_CoverageInvalidEdges(boundary, 0.0) OVER () AS bad_edge
        FROM zones WHERE level = 2
    ) edges
    WHERE bad_edge IS NOT NULL;
    IF invalid_edge_metres > 2000 THEN
        RAISE EXCEPTION 'Nespojenih coverage ivica na nivou 2: % m (prag 2000 m) — '
            'proveri da generator koristi ST_CoverageSimplify', round(invalid_edge_metres);
    END IF;
    RAISE NOTICE 'Nespojenih coverage ivica na nivou 2: % m', round(invalid_edge_metres);
END $$;

-- parent_name ostaje NULL kad se dete zove isto kao opstina (naseljeno mesto Obrenovac u
-- opstini Obrenovac, isto Lazarevac, Mladenovac, Sopot, Grocka, Surcin). Bez toga bi
-- labela bila "Obrenovac, Obrenovac"; ovako pada na grad i daje "Obrenovac, Beograd".
UPDATE zones child
SET parent_name = parent.name
FROM zones parent
WHERE parent.id = child.parent_id
  AND parent.name IS DISTINCT FROM child.name;

UPDATE zones
SET centroid_latitude = ST_Y(ST_PointOnSurface(boundary)),
    centroid_longitude = ST_X(ST_PointOnSurface(boundary)),
    area_km2 = ST_Area(boundary::geography) / 1e6
WHERE level = 2;

ALTER TABLE zones
    ADD CONSTRAINT ck_zones_level CHECK (level IN (1, 2));

ALTER TABLE zones
    ADD CONSTRAINT ck_zones_parent_by_level CHECK (
        (level = 1 AND parent_id IS NULL) OR (level = 2 AND parent_id IS NOT NULL));
