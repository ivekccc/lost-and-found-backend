CREATE TABLE locations (
                           id BIGSERIAL PRIMARY KEY,
                           latitude DECIMAL(10, 8) NOT NULL,
                           longitude DECIMAL(11, 8) NOT NULL,
                           formatted_address VARCHAR(500) NOT NULL,
                           country VARCHAR(100),
                           city VARCHAR(100),
                           district VARCHAR(100),
                           street VARCHAR(200),
                           osm_id VARCHAR(50),
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE INDEX idx_locations_city ON locations(city);
CREATE INDEX idx_locations_district ON locations(district);
CREATE INDEX idx_locations_city_district ON locations(city, district);
CREATE INDEX idx_locations_coordinates ON locations(latitude, longitude);
CREATE INDEX idx_locations_osm_id ON locations(osm_id);