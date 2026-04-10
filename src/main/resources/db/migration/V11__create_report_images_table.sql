CREATE TABLE report_images (
                               id BIGSERIAL PRIMARY KEY,
                               report_id BIGINT NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
                               image_url VARCHAR(500) NOT NULL,
                               public_id VARCHAR(255) NOT NULL,
                               display_order INT NOT NULL DEFAULT 0,
                               created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_report_images_report_id ON report_images(report_id);