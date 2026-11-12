ALTER TABLE matching_config
    ADD COLUMN trigram_share  NUMERIC(3, 2) NOT NULL DEFAULT 0.50
        CONSTRAINT matching_config_trigram_share_range CHECK (trigram_share BETWEEN 0 AND 1),
    ADD COLUMN semantic_floor NUMERIC(3, 2) NOT NULL DEFAULT 0.40
        CONSTRAINT matching_config_semantic_floor_range CHECK (semantic_floor BETWEEN 0 AND 0.95);
