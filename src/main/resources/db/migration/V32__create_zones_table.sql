CREATE TABLE zones (
    id                 BIGSERIAL PRIMARY KEY,
    code               VARCHAR(20) NOT NULL,
    name               VARCHAR(100) NOT NULL,
    city               VARCHAR(100) NOT NULL,
    boundary           geometry(MultiPolygon, 4326) NOT NULL,
    centroid_latitude  NUMERIC(10, 8) NOT NULL,
    centroid_longitude NUMERIC(11, 8) NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_zones_code UNIQUE (code)
);

CREATE INDEX idx_zones_boundary ON zones USING GIST (boundary);
