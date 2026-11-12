CREATE TABLE report_text_embeddings
(
    report_id  BIGINT PRIMARY KEY REFERENCES reports (id) ON DELETE CASCADE,
    model      VARCHAR(120) NOT NULL,
    embedding  REAL[]       NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
