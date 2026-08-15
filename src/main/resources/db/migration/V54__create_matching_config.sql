CREATE TABLE matching_config (
    id              BIGINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    max_distance_km NUMERIC(6, 2) NOT NULL CHECK (max_distance_km > 0),
    weight_distance SMALLINT NOT NULL CHECK (weight_distance BETWEEN 0 AND 100),
    weight_text     SMALLINT NOT NULL CHECK (weight_text BETWEEN 0 AND 100),
    weight_time     SMALLINT NOT NULL CHECK (weight_time BETWEEN 0 AND 100),
    time_decay_days SMALLINT NOT NULL CHECK (time_decay_days > 0),
    score_threshold SMALLINT NOT NULL CHECK (score_threshold BETWEEN 0 AND 100),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT matching_config_weights_sum
        CHECK (weight_distance + weight_text + weight_time = 100)
);

INSERT INTO matching_config (id, max_distance_km, weight_distance, weight_text,
                             weight_time, time_decay_days, score_threshold)
VALUES (1, 25.00, 40, 35, 25, 30, 40);
