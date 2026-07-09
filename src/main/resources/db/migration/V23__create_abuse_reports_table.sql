CREATE TABLE abuse_reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users (id),
    target_type VARCHAR(20) NOT NULL,
    target_user_id BIGINT REFERENCES users (id),
    target_report_id BIGINT REFERENCES reports (id),
    reason VARCHAR(30) NOT NULL,
    message VARCHAR(1000),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reviewed_by BIGINT REFERENCES users (id),
    reviewed_at TIMESTAMP,
    resolution_note VARCHAR(1000)
);

CREATE INDEX idx_abuse_reports_status ON abuse_reports (status);
CREATE INDEX idx_abuse_reports_reporter ON abuse_reports (reporter_id);
CREATE INDEX idx_abuse_reports_target_user ON abuse_reports (target_user_id);
CREATE INDEX idx_abuse_reports_target_report ON abuse_reports (target_report_id);
