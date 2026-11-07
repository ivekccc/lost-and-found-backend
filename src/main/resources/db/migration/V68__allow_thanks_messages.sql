ALTER TABLE messages DROP CONSTRAINT ck_messages_type;

ALTER TABLE messages ADD CONSTRAINT ck_messages_type
    CHECK (type IN ('TEXT', 'MEETING_POINT', 'CONTACT', 'THANKS'));

CREATE UNIQUE INDEX uq_messages_thanks_per_conversation ON messages (conversation_id) WHERE type = 'THANKS';
