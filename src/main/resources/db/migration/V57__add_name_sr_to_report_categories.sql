ALTER TABLE report_categories ADD COLUMN name_sr VARCHAR(100);
UPDATE report_categories SET name_sr = name WHERE name_sr IS NULL;
ALTER TABLE report_categories ALTER COLUMN name_sr SET NOT NULL;
