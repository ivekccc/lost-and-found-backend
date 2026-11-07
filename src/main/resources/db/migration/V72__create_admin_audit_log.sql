CREATE TABLE admin_audit_log (
    id          BIGSERIAL PRIMARY KEY,
    actor_id    BIGINT REFERENCES users (id) ON DELETE SET NULL,
    actor_name  VARCHAR(255) NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    target_type VARCHAR(30)  NOT NULL,
    target_id   BIGINT,
    summary     VARCHAR(500) NOT NULL,
    details     JSONB,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_audit_log_created_at ON admin_audit_log (created_at DESC);
CREATE INDEX idx_admin_audit_log_target ON admin_audit_log (target_type, target_id, created_at DESC);
CREATE INDEX idx_admin_audit_log_actor ON admin_audit_log (actor_id, created_at DESC);
