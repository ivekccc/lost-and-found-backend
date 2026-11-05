ALTER TABLE question_templates ADD COLUMN prompt_sr VARCHAR(500);
ALTER TABLE question_templates ADD COLUMN default_choices_sr JSONB;
UPDATE question_templates SET prompt_sr = prompt, default_choices_sr = default_choices WHERE prompt_sr IS NULL;
ALTER TABLE question_templates ALTER COLUMN prompt_sr SET NOT NULL;
