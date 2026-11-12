ALTER TABLE report_matches
    ADD COLUMN trigram_similarity     NUMERIC(4, 3),
    ADD COLUMN semantic_similarity    NUMERIC(4, 3),
    ADD COLUMN applied_trigram_share  NUMERIC(3, 2),
    ADD COLUMN applied_semantic_floor NUMERIC(3, 2);

UPDATE report_matches
SET trigram_similarity     = text_similarity,
    applied_trigram_share  = 1.00,
    applied_semantic_floor = 0.40
WHERE trigram_similarity IS NULL;

ALTER TABLE report_matches
    ALTER COLUMN trigram_similarity SET NOT NULL,
    ALTER COLUMN applied_trigram_share SET NOT NULL,
    ALTER COLUMN applied_semantic_floor SET NOT NULL;
