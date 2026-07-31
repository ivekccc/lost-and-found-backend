-- Jedan izvor istine za "koja je zona ove tacke".
--
-- Do sada je pravilo postojalo na dva mesta koja su se vec razisla: V34 backfill je
-- koristio ST_Contains bez fallback-a, a ZoneRepository ST_Covers + fallback. Svaka
-- buduca izmena runtime upita tiho bi produbila razliku. Sada i aplikacija i migracije
-- zovu ovu funkciju.
--
-- RETURNS TABLE (a ne skalarni RETURNS bigint): skalarna funkcija koja vrati NULL daje
-- JEDAN red sa null vrednoscu, pa mapiranje u Optional zavisi od verzije Spring Data.
-- Set-returning funkcija vraca NULA redova, sto se mapira u Optional.empty() jednoznacno.
--
-- Izlazna kolona se zove zone_id, ne id: u SQL funkcijama sa RETURNS TABLE imena izlaznih
-- kolona su vidljiva u telu i zasenila bi zones.id u nekvalifikovanim referencama.

CREATE OR REPLACE FUNCTION zone_resolve(p_latitude double precision,
                                        p_longitude double precision)
RETURNS TABLE (zone_id bigint)
LANGUAGE sql
STABLE
PARALLEL SAFE
AS $fn$
    WITH probe AS (
        SELECT ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326) AS geom
    ),
    covering_parent AS (
        SELECT z.id AS parent_zone_id
        FROM zones z, probe
        WHERE z.level = 1
          AND ST_Covers(z.boundary, probe.geom)
        ORDER BY z.id
        LIMIT 1
    ),
    candidates AS (
        -- tier 0: tacka je unutar jedinice nivoa 2 — najbolji moguci odgovor.
        SELECT z.id AS candidate_id, 0 AS tier, 0 AS distance, z.area_km2 AS area
        FROM zones z, probe
        WHERE z.level = 2
          AND ST_Covers(z.boundary, probe.geom)

        UNION ALL

        -- tier 1: procep izmedu dve jedinice nivoa 2 nastao uproscavanjem granica.
        -- 0.0002 stepena = 22 m po latitudi / 16 m po longitudi na 44.8N — dovoljno da
        -- prede procep, premalo da stigne do susedne mesne zajednice. Ograniceno na DECU
        -- opstine koja vec pokriva tacku, pa je najgori ishod "jedinica sa druge strane
        -- linije", sto je za tacku NA liniji ionako tacan odgovor.
        SELECT z.id, 1, ST_Distance(z.boundary, probe.geom), z.area_km2
        FROM zones z, probe
        WHERE z.level = 2
          AND z.parent_id = (SELECT parent_zone_id FROM covering_parent)
          AND ST_DWithin(z.boundary, probe.geom, 0.0002)

        UNION ALL

        -- tier 2: prava rupa u podacima (Ada Ciganlija, Kosutnjak, kej, industrijske
        -- parcele) — nijedna mesna zajednica je ne pokriva. Posteno je imenovati opstinu;
        -- imenovati mesnu zajednicu 300 m dalje bila bi netacna javna tvrdnja o lokaciji.
        SELECT z.id, 2, 0, z.area_km2
        FROM zones z, probe
        WHERE z.level = 1
          AND ST_Covers(z.boundary, probe.geom)

        UNION ALL

        -- tier 3: tacka malo van granice grada. Zatecno ponasanje, samo na nivou 1.
        SELECT z.id, 3, ST_Distance(z.boundary, probe.geom), z.area_km2
        FROM zones z, probe
        WHERE z.level = 1
          AND ST_DWithin(z.boundary, probe.geom, 0.003)
    )
    -- Redosled tirova je nosivi deo: "bilo koji ST_Covers pa ORDER BY level DESC" dao bi
    -- tacki 3 m unutar procepa opstinu umesto ocigledno tacne susedne mesne zajednice.
    -- Manja povrsina pobeduje pri izjednacenju, pa je ishod deterministican i kad se
    -- geometrija pomeri pri buducem osvezavanju.
    SELECT c.candidate_id
    FROM candidates c
    ORDER BY c.tier, c.distance, c.area, c.candidate_id
    LIMIT 1;
$fn$;
