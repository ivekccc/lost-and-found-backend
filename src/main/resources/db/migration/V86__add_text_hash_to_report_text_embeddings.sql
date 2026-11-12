DELETE FROM report_text_embeddings;

ALTER TABLE report_text_embeddings
    ADD COLUMN text_hash VARCHAR(64) NOT NULL;
