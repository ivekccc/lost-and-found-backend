ALTER TABLE users ADD COLUMN google_sub VARCHAR(64);

ALTER TABLE users ADD CONSTRAINT uq_users_google_sub UNIQUE (google_sub);
