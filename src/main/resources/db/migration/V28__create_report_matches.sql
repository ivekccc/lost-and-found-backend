CREATE TABLE report_matches (
    id                 BIGSERIAL PRIMARY KEY,
    lost_report_id     BIGINT NOT NULL REFERENCES reports (id) ON DELETE CASCADE,
    found_report_id    BIGINT NOT NULL REFERENCES reports (id) ON DELETE CASCADE,
    score              INTEGER NOT NULL,
    distance_km        NUMERIC(6, 2) NOT NULL,
    distance_score     INTEGER NOT NULL,
    time_gap_days      INTEGER NOT NULL,
    time_score         INTEGER NOT NULL,
    text_similarity    NUMERIC(4, 3) NOT NULL,
    text_score         INTEGER NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'SUGGESTED',
    lost_dismissed_at  TIMESTAMP,
    found_dismissed_at TIMESTAMP,
    notified_at        TIMESTAMP,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_report_matches_pair UNIQUE (lost_report_id, found_report_id)
);

CREATE INDEX idx_report_matches_lost_report_id ON report_matches (lost_report_id);
CREATE INDEX idx_report_matches_found_report_id ON report_matches (found_report_id);
CREATE INDEX idx_report_matches_status ON report_matches (status);
CREATE INDEX idx_report_matches_score ON report_matches (score DESC);
