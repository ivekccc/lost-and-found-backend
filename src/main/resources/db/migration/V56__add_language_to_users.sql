-- Server-initiated messages (push, inbox, email) have no request to take a language from, so the
-- recipient's choice is stored. Existing accounts start on Serbian, the app's home market.
ALTER TABLE users ADD COLUMN language VARCHAR(10) NOT NULL DEFAULT 'SR_LATN';
