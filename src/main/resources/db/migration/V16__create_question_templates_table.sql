-- Predefined verification questions per category, offered when composing a challenge
CREATE TABLE question_templates (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES report_categories(id),
    prompt VARCHAR(500) NOT NULL,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('TEXT', 'CHOICE')),
    default_choices JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT choice_templates_require_choices
        CHECK (kind <> 'CHOICE' OR default_choices IS NOT NULL)
);

CREATE INDEX idx_question_templates_category_id ON question_templates(category_id);
