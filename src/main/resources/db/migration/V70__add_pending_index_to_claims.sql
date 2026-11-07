CREATE INDEX idx_claims_pending_submitted_at ON claims (submitted_at) WHERE status = 'PENDING';
