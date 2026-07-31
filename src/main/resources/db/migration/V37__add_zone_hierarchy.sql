-- Hijerarhija zona: nivo 1 = gradska opstina, nivo 2 = mesna zajednica / naseljeno mesto.
--
-- Maskiranje lokacije je do sada bilo na nivou opstine, sto je prekrupno da bi bilo
-- korisno (Palilula 451 km2). Nivo 2 ima medijanu ~0.8 km2. Nivo 1 ostaje kao fallback
-- za tacke koje ne padaju ni u jednu jedinicu nivoa 2 (parkovi, industrijske zone,
-- obale) pa pokrivenost ostaje 100%.
--
-- CHECK constraint-i za level/parent_id NAMERNO nisu ovde: PostgreSQL CHECK ne moze biti
-- DEFERRABLE, pa bi "level = 2 => parent_id IS NOT NULL" oborio svaki INSERT u V38, gde
-- se deca unose pre nego sto im se roditelj moze izracunati. Dodaju se u V39, posle
-- dodele roditelja.

-- parent_name je denormalizovan namerno. Labela zone ("Mirijevo, Zvezdara") gradi se za
-- SVAKI oglas u listi, pa bi dohvatanje imena roditelja kroz zaseban upit ili kroz
-- asocijaciju bilo N+1 po listi oglasa. Popunjava se u V39, zajedno sa parent_id.
ALTER TABLE zones
    ADD COLUMN parent_id       BIGINT,
    ADD COLUMN parent_name     VARCHAR(100),
    ADD COLUMN level           SMALLINT NOT NULL DEFAULT 1,
    ADD COLUMN osm_relation_id BIGINT,
    ADD COLUMN area_km2        NUMERIC(10, 4);

ALTER TABLE zones
    ALTER COLUMN level DROP DEFAULT;

-- Kodovi nivoa 2 su oblika <kod_opstine>-<slug>, npr. BG-ZVE-BRACE-JERKOVIC (21 znak),
-- pa zatecenih VARCHAR(20) ne bi bilo dovoljno. Sirenje varchar-a je izmena samo u
-- katalogu: bez rewrite-a tabele i bez rebuild-a indeksa.
ALTER TABLE zones
    ALTER COLUMN code TYPE VARCHAR(40);

-- osm_relation_id je stabilan identitet jedinice: ime se u OSM-u menja, id ne. Zahvaljujuci
-- njemu buduci refresh geometrije moze da ide kao INSERT ... ON CONFLICT DO UPDATE, sto
-- azurira redove u mestu i nikada ne pomera zones.id — jedini oblik osvezavanja koji ne
-- ugrozava FK iz locations.zone_id.
UPDATE zones z
SET osm_relation_id = v.relation_id
FROM (VALUES
    ('BG-BAR', 3065610),
    ('BG-CUK', 7727516),
    ('BG-GRO', 3065611),
    ('BG-LAZ', 3059654),
    ('BG-MLA', 2223784),
    ('BG-NBG', 3085749),
    ('BG-OBR', 3059655),
    ('BG-PAL', 7737269),
    ('BG-RAK', 7728561),
    ('BG-SAV', 5892891),
    ('BG-SOP', 3065612),
    ('BG-STG', 2027114),
    ('BG-SUR', 3085424),
    ('BG-VOZ', 7737268),
    ('BG-VRA', 5731335),
    ('BG-ZEM', 3085750),
    ('BG-ZVE', 6930224)
) AS v(code, relation_id)
WHERE z.code = v.code;

DO $$
DECLARE missing INT;
BEGIN
    SELECT count(*) INTO missing FROM zones WHERE osm_relation_id IS NULL;
    IF missing > 0 THEN
        RAISE EXCEPTION '% zona bez osm_relation_id — kodovi opstina se ne poklapaju', missing;
    END IF;
END $$;

ALTER TABLE zones
    ALTER COLUMN osm_relation_id SET NOT NULL;

ALTER TABLE zones
    ADD CONSTRAINT uq_zones_osm_relation_id UNIQUE (osm_relation_id);

ALTER TABLE zones
    ADD CONSTRAINT fk_zones_parent FOREIGN KEY (parent_id) REFERENCES zones (id);

CREATE INDEX idx_zones_parent_id ON zones (parent_id);
CREATE INDEX idx_zones_level ON zones (level);

-- area_km2 je denormalizovan da bi razresavanje zone moglo da ga koristi kao deterministicki
-- tiebreak bez racunanja ST_Area po kandidatu, i da bi prag minimalne povrsine pri prikazu
-- bio obican upit. Racuna se posle V36, koja popravlja granice nivoa 1.
UPDATE zones
SET area_km2 = ST_Area(boundary::geography) / 1e6;
