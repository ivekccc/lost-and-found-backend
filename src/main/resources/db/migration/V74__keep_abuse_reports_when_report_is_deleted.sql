ALTER TABLE abuse_reports DROP CONSTRAINT abuse_reports_target_report_id_fkey;

ALTER TABLE abuse_reports
    ADD CONSTRAINT abuse_reports_target_report_id_fkey
    FOREIGN KEY (target_report_id) REFERENCES reports (id) ON DELETE SET NULL;
