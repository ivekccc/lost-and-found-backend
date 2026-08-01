-- Dovrsavanje zona novih gradova i racunanje okvira grada.
--
-- V45 i V46 unose samo ono sto dolazi iz OSM-a: kod, ime, granicu i roditelja. Sve izvedeno
-- se racuna ovde, jednom mestu, da se izracunavanje ne bi ponavljalo u svakom seed fajlu i
-- razislo se izmedu gradova — greska koja je vec jednom napravljena sa razresavanjem zone
-- (V34 protiv ZoneRepository), zbog cega V40 postoji.

-- parent_name ostaje NULL kad se dete zove isto kao roditelj: naseljeno mesto "Bajina Bašta"
-- u opstini "Bajina Bašta" dalo bi labelu "Bajina Bašta, Bajina Bašta". Ovako pada na grad.
UPDATE zones child
SET parent_name = parent.name
FROM zones parent
WHERE parent.id = child.parent_id
  AND parent.name IS DISTINCT FROM child.name
  AND child.parent_name IS NULL;

-- Centroid je ST_PointOnSurface, ne ST_Centroid: kod izduzene ili udubljene zone (opstina
-- uz meandar Drine) centroid ume da padne izvan sopstvene granice, pa bi "centar zone" bio
-- tacka u susednoj opstini.
UPDATE zones z
SET centroid_latitude = ST_Y(ST_PointOnSurface(z.boundary)),
    centroid_longitude = ST_X(ST_PointOnSurface(z.boundary)),
    area_km2 = ST_Area(z.boundary::geography) / 1e6
FROM cities c
WHERE c.id = z.city_id
  AND c.code IN ('NS', 'BB');

-- Okvir grada se izvodi iz unije njegovih zona nivoa 1, a ne iz zasebno dohvacene granice
-- grada: time je nemoguce da okvir i zone opisuju razlicito podrucje. Okvir ide u LocationIQ
-- kao viewbox (bounded=1), pa bi manjak nekoliko kilometara znacio adresu koju korisnik vidi
-- na mapi a ne moze da je izabere.
--
-- Za Beograd ovo usput popravlja zatecenu gresku: LocationService je imao hardkodiran
-- viewbox 20.22,44.93,20.65,44.68, sto je samo urbano jezgro — adrese u Obrenovcu,
-- Lazarevcu, Mladenovcu i Sopotu (preko polovine povrsine grada) pretraga nije nalazila.
UPDATE cities c
SET center_latitude    = ST_Y(ST_PointOnSurface(extent.geom)),
    center_longitude   = ST_X(ST_PointOnSurface(extent.geom)),
    bbox_min_latitude  = ST_YMin(extent.geom),
    bbox_min_longitude = ST_XMin(extent.geom),
    bbox_max_latitude  = ST_YMax(extent.geom),
    bbox_max_longitude = ST_XMax(extent.geom)
FROM (
    SELECT z.city_id, ST_Union(z.boundary) AS geom
    FROM zones z
    WHERE z.level = 1
    GROUP BY z.city_id
) extent
WHERE extent.city_id = c.id;

DO $$
DECLARE
    incomplete INT;
    strays INT;
    overlapping INT;
    invalid_edge_metres NUMERIC;
BEGIN
    SELECT count(*) INTO incomplete FROM cities WHERE bbox_min_latitude IS NULL;
    IF incomplete > 0 THEN
        RAISE EXCEPTION '% grada nema okvir — grad bez ijedne zone nivoa 1', incomplete;
    END IF;

    -- Dete manje od 80%% unutar roditelja znaci da je jedinica dohvacena iz pogresnog
    -- podrucja. Kod novih gradova roditelj nije biran po preseku nego je poznat po
    -- konstrukciji, pa ovo proverava da OSM upit nije pokupio susednu opstinu.
    SELECT count(*) INTO strays
    FROM zones child
    JOIN zones parent ON parent.id = child.parent_id
    JOIN cities c ON c.id = child.city_id
    WHERE c.code IN ('NS', 'BB')
      AND ST_Area(ST_Intersection(parent.boundary, child.boundary))
          < 0.80 * ST_Area(child.boundary);
    IF strays > 0 THEN
        RAISE EXCEPTION '% zona nivoa 2 je manje od 80%% unutar svog grada', strays;
    END IF;

    -- Nivo 2 mora biti disjunktan unutar grada, inace razresavanje zone zavisi od tiebreak-a
    -- i ista tacka moze dobiti razlicit odgovor posle osvezavanja geometrije. Parovi se ne
    -- porede preko granica gradova — dva grada se ionako ne dodiruju.
    SELECT count(*) INTO overlapping
    FROM zones a
    JOIN zones b ON a.id < b.id AND a.city_id = b.city_id
    JOIN cities c ON c.id = a.city_id
    WHERE c.code IN ('NS', 'BB')
      AND a.level = 2 AND b.level = 2
      AND ST_Area(ST_CollectionExtract(ST_Intersection(a.boundary, b.boundary), 3)::geography) > 500;
    IF overlapping > 0 THEN
        RAISE EXCEPTION '% parova zona nivoa 2 se preklapa preko 500 m2', overlapping;
    END IF;

    -- Prag hvata regresiju na per-feature uproscavanje (ono bi dalo desetine kilometara
    -- neuparenih ivica), a tolerise stvarne rupe izmedu nezavisno mapiranih seoskih naselja.
    -- Tacka u takvoj rupi pada na tier 2 u zone_resolve i dobija ime grada, sto je posteno.
    SELECT coalesce(sum(ST_Length(bad_edge::geography)), 0) INTO invalid_edge_metres
    FROM (
        SELECT ST_CoverageInvalidEdges(z.boundary, 0.0) OVER (PARTITION BY z.city_id) AS bad_edge
        FROM zones z
        JOIN cities c ON c.id = z.city_id
        WHERE z.level = 2 AND c.code IN ('NS', 'BB')
    ) edges
    WHERE bad_edge IS NOT NULL;
    IF invalid_edge_metres > 2000 THEN
        RAISE EXCEPTION 'Nespojenih coverage ivica na nivou 2: % m (prag 2000 m) — '
            'proveri da generator koristi ST_CoverageSimplify', round(invalid_edge_metres);
    END IF;
    RAISE NOTICE 'Nespojenih coverage ivica na nivou 2 novih gradova: % m',
        round(invalid_edge_metres);
END $$;

ALTER TABLE cities
    ALTER COLUMN center_latitude SET NOT NULL,
    ALTER COLUMN center_longitude SET NOT NULL,
    ALTER COLUMN bbox_min_latitude SET NOT NULL,
    ALTER COLUMN bbox_min_longitude SET NOT NULL,
    ALTER COLUMN bbox_max_latitude SET NOT NULL,
    ALTER COLUMN bbox_max_longitude SET NOT NULL;
