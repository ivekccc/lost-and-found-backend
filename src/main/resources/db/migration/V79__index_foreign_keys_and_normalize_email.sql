CREATE INDEX idx_abuse_reports_reviewed_by ON abuse_reports (reviewed_by);
CREATE INDEX idx_challenge_questions_template_id ON challenge_questions (template_id);
CREATE INDEX idx_claim_answers_challenge_question_id ON claim_answers (challenge_question_id);
CREATE INDEX idx_conversations_report_id ON conversations (report_id);
CREATE INDEX idx_saved_reports_report_id ON saved_reports (report_id);
CREATE INDEX idx_saved_searches_category_id ON saved_searches (category_id);
CREATE INDEX idx_saved_searches_zone_id ON saved_searches (zone_id);
CREATE INDEX idx_users_active_city_id ON users (active_city_id);

DROP INDEX idx_users_email;

UPDATE users SET email = lower(trim(email)) WHERE email <> lower(trim(email));
UPDATE password_resets SET email = lower(trim(email)) WHERE email <> lower(trim(email));
UPDATE pre_registrations SET email = lower(trim(email)) WHERE email <> lower(trim(email));

ALTER TABLE users ADD CONSTRAINT chk_users_email_lowercase CHECK (email = lower(email));
