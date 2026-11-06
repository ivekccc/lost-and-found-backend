ALTER TABLE reports ADD COLUMN share_token UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX idx_reports_share_token ON reports (share_token);
