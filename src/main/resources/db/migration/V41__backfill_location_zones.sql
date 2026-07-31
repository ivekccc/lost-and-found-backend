-- Preusmeravanje postojecih lokacija na NAJDUBLJU zonu koja ih sadrzi.
--
-- Posle V34 su sve lokacije pokazivale na opstinu; ta zona je od sada fallback, ne odgovor.
-- Uz to, V34 backfill je koristio ST_Contains bez fallback-a dok je runtime koristio
-- ST_Covers + fallback, pa je nekoliko lokacija na granici ostalo sa zone_id = NULL iako
-- bi ih aplikacija razresila. zone_resolve() je sada jedini izvor istine za oba puta, pa
-- ovaj prolaz popravlja i te redove.
--
-- OBAVEZNO uz svako budce osvezavanje geometrije zona: ReportService.findOrCreateLocation
-- dedupe-uje lokacije po osm_id i NIKADA ne preracunava zonu postojeceg reda, pa ispravnost
-- zatecenih podataka zavisi iskljucivo od ovakvog backfill-a.
--
-- Napomena o obliku upita: UPDATE ... FROM LATERAL koji referise ciljnu tabelu PostgreSQL
-- odbija ("invalid reference to FROM-clause entry"), pa razresavanje ide kroz CTE spojen
-- po locations.id.

WITH resolved AS (
    SELECT l.id AS location_id,
           (SELECT r.zone_id
            FROM zone_resolve(l.latitude::double precision, l.longitude::double precision) r)
               AS new_zone_id
    FROM locations l
)
UPDATE locations l
SET zone_id = resolved.new_zone_id
FROM resolved
WHERE resolved.location_id = l.id
  AND resolved.new_zone_id IS NOT NULL
  AND resolved.new_zone_id IS DISTINCT FROM l.zone_id;

DO $$
DECLARE
    on_level2 INT;
    on_level1 INT;
    unresolved INT;
BEGIN
    SELECT count(*) INTO on_level2
    FROM locations l JOIN zones z ON z.id = l.zone_id WHERE z.level = 2;

    SELECT count(*) INTO on_level1
    FROM locations l JOIN zones z ON z.id = l.zone_id WHERE z.level = 1;

    SELECT count(*) INTO unresolved FROM locations WHERE zone_id IS NULL;

    RAISE NOTICE 'Lokacije po nivou zone: nivo 2 = %, nivo 1 (rupe u podacima) = %, bez zone (van Beograda) = %',
        on_level2, on_level1, unresolved;
END $$;
