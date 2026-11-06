CREATE TABLE saved_searches (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    city_id     BIGINT       NOT NULL REFERENCES cities (id),
    type        VARCHAR(20),
    category_id BIGINT REFERENCES report_categories (id),
    zone_id     BIGINT REFERENCES zones (id),
    search_text VARCHAR(100),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_saved_searches_type CHECK (type IS NULL OR type IN ('LOST', 'FOUND')),
    CONSTRAINT ck_saved_searches_not_empty CHECK (
        type IS NOT NULL OR category_id IS NOT NULL OR zone_id IS NOT NULL OR search_text IS NOT NULL)
);

CREATE INDEX idx_saved_searches_user_id ON saved_searches (user_id);
CREATE INDEX idx_saved_searches_city_id ON saved_searches (city_id);
