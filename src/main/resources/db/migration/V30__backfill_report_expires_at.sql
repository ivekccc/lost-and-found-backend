UPDATE reports
SET expires_at = created_at + INTERVAL '60 days'
WHERE expires_at IS NULL;
