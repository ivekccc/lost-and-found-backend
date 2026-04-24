CREATE TABLE contact_reveals
(
    id          BIGSERIAL PRIMARY KEY,
    report_id   BIGINT      NOT NULL REFERENCES reports (id) ON DELETE CASCADE,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(500),
    revealed_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contact_reveals_user_revealed_at ON contact_reveals (user_id, revealed_at);

CREATE INDEX idx_contact_reveals_report_id ON contact_reveals (report_id);
