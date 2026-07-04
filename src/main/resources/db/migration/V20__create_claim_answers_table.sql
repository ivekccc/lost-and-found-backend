-- Claimant's answer to a single challenge question.
-- is_correct is auto-filled for CHOICE questions, NULL for TEXT (manual review).
CREATE TABLE claim_answers (
    id BIGSERIAL PRIMARY KEY,
    claim_id BIGINT NOT NULL REFERENCES claims(id),
    challenge_question_id BIGINT NOT NULL REFERENCES challenge_questions(id),
    answer_text VARCHAR(1000) NOT NULL,
    is_correct BOOLEAN,

    CONSTRAINT uq_claim_answers_claim_question UNIQUE (claim_id, challenge_question_id)
);

CREATE INDEX idx_claim_answers_claim_id ON claim_answers(claim_id);
