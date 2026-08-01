-- Veza zone sa gradom.
--
-- Tekstualna kolona zones.city OSTAJE i posle ovoga. Nije zaostatak: iz nje se gradi labela
-- lokacije za SVAKI oglas u listi (ReportZoneDto), pa bi dohvatanje imena grada kroz JOIN
-- bilo N+1 po listi. Isti razlog zbog kog V37 drzi denormalizovan parent_name. Identitet i
-- pravila su na city_id, prikaz na tekstu.
ALTER TABLE zones
    ADD COLUMN city_id BIGINT;

UPDATE zones z
SET city_id = c.id
FROM cities c
WHERE c.name = z.city;

-- Sve zatecene zone su beogradske, pa nesparen red znaci da je ime grada negde razidjeno
-- sa cities.name — bolje pasti ovde nego pustiti NOT NULL da baci opskurnu poruku.
DO $$
DECLARE unmatched INT;
BEGIN
    SELECT count(*) INTO unmatched FROM zones WHERE city_id IS NULL;
    IF unmatched > 0 THEN
        RAISE EXCEPTION '% zona nije spareno sa gradom po imenu', unmatched;
    END IF;
END $$;

ALTER TABLE zones
    ALTER COLUMN city_id SET NOT NULL;

ALTER TABLE zones
    ADD CONSTRAINT fk_zones_city FOREIGN KEY (city_id) REFERENCES cities (id);

-- Svaki upit nad oglasima od sada nosi uslov po gradu (ReportSpecifications.inCity), a
-- stize do zone preko locations.zone_id — pa je ovo indeks po kome se filtrira, ne ukras.
CREATE INDEX idx_zones_city_id ON zones (city_id);
