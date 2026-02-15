-- Pre-registrations table for email verification
CREATE TABLE pre_registrations (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    verification_code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pre_registrations_email ON pre_registrations(email);
CREATE INDEX idx_pre_registrations_code ON pre_registrations(verification_code);
