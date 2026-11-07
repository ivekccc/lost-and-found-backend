DELETE FROM password_resets;
DELETE FROM pre_registrations;

ALTER TABLE password_resets RENAME COLUMN code TO code_hash;
ALTER TABLE password_resets ALTER COLUMN code_hash TYPE VARCHAR(64);
ALTER TABLE password_resets ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0;

ALTER TABLE pre_registrations RENAME COLUMN verification_code TO code_hash;
ALTER TABLE pre_registrations ALTER COLUMN code_hash TYPE VARCHAR(64);
ALTER TABLE pre_registrations ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0;
