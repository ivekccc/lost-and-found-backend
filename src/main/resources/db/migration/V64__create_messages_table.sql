CREATE TABLE messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT        NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    sender_id       BIGINT        NOT NULL REFERENCES users (id),
    type            VARCHAR(20)   NOT NULL,
    body            VARCHAR(2000),
    latitude        NUMERIC(10, 8),
    longitude       NUMERIC(11, 8),
    place_label     VARCHAR(300),
    contact_name    VARCHAR(200),
    contact_email   VARCHAR(255),
    contact_phone   VARCHAR(50),
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_messages_type CHECK (type IN ('TEXT', 'MEETING_POINT', 'CONTACT'))
);

CREATE INDEX idx_messages_conversation_id ON messages (conversation_id, id);
CREATE INDEX idx_messages_sender_created ON messages (sender_id, created_at);
