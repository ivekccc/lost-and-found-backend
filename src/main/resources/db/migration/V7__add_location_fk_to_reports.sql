ALTER TABLE reports ADD COLUMN location_id BIGINT;


ALTER TABLE reports
    ADD CONSTRAINT fk_reports_location
        FOREIGN KEY (location_id)
            REFERENCES locations(id);


CREATE INDEX idx_reports_location_id ON reports(location_id);