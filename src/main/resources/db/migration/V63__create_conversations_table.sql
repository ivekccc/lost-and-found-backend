CREATE TABLE conversations (
    id               BIGSERIAL PRIMARY KEY,
    claim_id         BIGINT    NOT NULL REFERENCES claims (id) ON DELETE CASCADE,
    report_id        BIGINT    NOT NULL REFERENCES reports (id) ON DELETE CASCADE,
    holder_id        BIGINT    NOT NULL REFERENCES users (id),
    claimant_id      BIGINT    NOT NULL REFERENCES users (id),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    last_message_at  TIMESTAMP,
    holder_read_at   TIMESTAMP,
    claimant_read_at TIMESTAMP,
    CONSTRAINT uq_conversations_claim UNIQUE (claim_id)
);

CREATE INDEX idx_conversations_holder_id ON conversations (holder_id);
CREATE INDEX idx_conversations_claimant_id ON conversations (claimant_id);

INSERT INTO conversations (claim_id, report_id, holder_id, claimant_id, created_at)
SELECT c.id, ch.report_id, ch.author_id, c.claimant_id, COALESCE(c.decided_at, NOW())
FROM claims c
JOIN challenges ch ON ch.id = c.challenge_id
JOIN reports r ON r.id = ch.report_id
WHERE c.status = 'APPROVED' AND r.status <> 'DELETED';
