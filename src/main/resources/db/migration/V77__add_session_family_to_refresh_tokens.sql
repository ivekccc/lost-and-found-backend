ALTER TABLE refresh_tokens ADD COLUMN family_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE refresh_tokens ADD COLUMN family_expires_at TIMESTAMP NOT NULL DEFAULT NOW() + INTERVAL '30 days';
ALTER TABLE refresh_tokens ADD COLUMN consumed_at TIMESTAMP;

ALTER TABLE refresh_tokens ALTER COLUMN family_id DROP DEFAULT;
ALTER TABLE refresh_tokens ALTER COLUMN family_expires_at DROP DEFAULT;

CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
