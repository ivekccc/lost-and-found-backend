-- A challenge is a set of verification questions authored by the item holder.
-- FOUND report: created together with the report, author = report poster.
-- LOST report: created by a finder via "I think I found this" (one per finder).
CREATE TABLE challenges (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES reports(id),
    author_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_challenges_report_author UNIQUE (report_id, author_id)
);

CREATE INDEX idx_challenges_report_id ON challenges(report_id);
CREATE INDEX idx_challenges_author_id ON challenges(author_id);
