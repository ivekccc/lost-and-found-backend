-- Minimum number of verification questions required when composing a challenge for this category
ALTER TABLE report_categories
    ADD COLUMN min_questions INT NOT NULL DEFAULT 2;
