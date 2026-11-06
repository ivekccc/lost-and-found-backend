ALTER TABLE abuse_reports
    ADD COLUMN target_message_id BIGINT REFERENCES messages (id) ON DELETE SET NULL;

CREATE INDEX idx_abuse_reports_target_message ON abuse_reports (target_message_id);
