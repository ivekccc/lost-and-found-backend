CREATE TABLE notifications
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users (id),
    type          VARCHAR(50)  NOT NULL,
    title         VARCHAR(255) NOT NULL,
    body          TEXT         NOT NULL,
    data_json     TEXT,
    is_read       BOOLEAN      NOT NULL DEFAULT FALSE,
    push_status   VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    push_error    TEXT,
    retry_count   INTEGER      NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP,
    sent_push_at  TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);

CREATE INDEX idx_notifications_user_unread ON notifications(user_id,is_read)
    WHERE is_read = FALSE;

CREATE INDEX idx_notifications_push_retry ON notifications(push_status, next_retry_at)
    WHERE push_status = 'FAILED';