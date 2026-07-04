-- Questions of a concrete challenge. Template questions are copied (snapshot);
-- template_id is kept only as provenance, so template edits never change past challenges.
CREATE TABLE challenge_questions (
    id BIGSERIAL PRIMARY KEY,
    challenge_id BIGINT NOT NULL REFERENCES challenges(id),
    prompt VARCHAR(500) NOT NULL,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('TEXT', 'CHOICE')),
    source VARCHAR(20) NOT NULL CHECK (source IN ('TEMPLATE', 'CUSTOM')),
    template_id BIGINT REFERENCES question_templates(id) ON DELETE SET NULL,
    choices JSONB,
    correct_answer VARCHAR(500),
    order_index INT NOT NULL,

    CONSTRAINT choice_questions_require_choices_and_answer
        CHECK (kind <> 'CHOICE' OR (choices IS NOT NULL AND correct_answer IS NOT NULL))
);

CREATE INDEX idx_challenge_questions_challenge_id ON challenge_questions(challenge_id);
