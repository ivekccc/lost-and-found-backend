ALTER TABLE reports
    ADD CONSTRAINT chk_reports_type CHECK (type IN ('LOST', 'FOUND')),
    ADD CONSTRAINT chk_reports_status CHECK (status IN ('ACTIVE', 'MATCHED', 'CLOSED', 'RESOLVED', 'EXPIRED', 'FLAGGED', 'DELETED'));

ALTER TABLE users
    ADD CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'PARTIALLY_BLOCKED', 'DELETED')),
    ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN', 'COORDINATOR')),
    ADD CONSTRAINT chk_users_language CHECK (language IN ('SR_LATN', 'EN'));

ALTER TABLE abuse_reports
    ADD CONSTRAINT chk_abuse_reports_target_type CHECK (target_type IN ('USER', 'REPORT', 'MESSAGE')),
    ADD CONSTRAINT chk_abuse_reports_reason CHECK (reason IN ('SCAM', 'SPAM', 'OFFENSIVE', 'PERSONAL_INFO', 'WRONG_CATEGORY', 'OTHER')),
    ADD CONSTRAINT chk_abuse_reports_status CHECK (status IN ('PENDING', 'REVIEWED_ACTIONED', 'DISMISSED'));

ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_push_status CHECK (push_status IN ('PENDING', 'SENT', 'FAILED', 'SKIPPED'));
