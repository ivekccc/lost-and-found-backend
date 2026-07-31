ALTER TABLE locations
    ADD COLUMN zone_id BIGINT REFERENCES zones (id);

CREATE INDEX idx_locations_zone_id ON locations (zone_id);

-- Backfill postojecih lokacija: zona se odreduje geometrijski, pa nema potrebe
-- za startup kodom. Lokacije van Beograda ostaju bez zone (NULL).
UPDATE locations l
SET zone_id = z.id
FROM zones z
WHERE l.zone_id IS NULL
  AND ST_Contains(z.boundary, ST_SetSRID(ST_MakePoint(l.longitude, l.latitude), 4326));
