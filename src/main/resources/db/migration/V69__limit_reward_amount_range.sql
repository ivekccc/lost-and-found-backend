ALTER TABLE reports DROP CONSTRAINT ck_reports_reward_amount;

ALTER TABLE reports ADD CONSTRAINT ck_reports_reward_amount
    CHECK (reward_amount IS NULL OR (reward_amount BETWEEN 100 AND 1000000 AND type = 'LOST'));
