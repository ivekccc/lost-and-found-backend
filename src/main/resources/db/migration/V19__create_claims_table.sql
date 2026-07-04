-- A claim is one ownership attempt: the claimant answers the challenge questions.
-- The challenge author (item holder) approves or declines it.
CREATE TABLE claims (
    id BIGSERIAL PRIMARY KEY,
    challenge_id BIGINT NOT NULL REFERENCES challenges(id),
    claimant_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'DECLINED', 'WITHDRAWN')),
    message VARCHAR(1000),
    photo_url VARCHAR(500),
    photo_public_id VARCHAR(255),
    submitted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    decided_at TIMESTAMP
);

CREATE INDEX idx_claims_challenge_id ON claims(challenge_id);
CREATE INDEX idx_claims_claimant_id ON claims(claimant_id);

-- At most one active (PENDING) claim per (challenge, claimant)
CREATE UNIQUE INDEX uq_claims_pending_per_claimant
    ON claims(challenge_id, claimant_id)
    WHERE status = 'PENDING';
