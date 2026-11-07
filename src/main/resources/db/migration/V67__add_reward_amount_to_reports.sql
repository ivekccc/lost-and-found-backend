ALTER TABLE reports ADD COLUMN reward_amount INTEGER;

ALTER TABLE reports ADD CONSTRAINT ck_reports_reward_amount
    CHECK (reward_amount IS NULL OR (reward_amount > 0 AND type = 'LOST'));
