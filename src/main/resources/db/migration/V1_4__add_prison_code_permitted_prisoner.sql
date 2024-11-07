ALTER TABLE permitted_prisoner ADD prison_code VARCHAR(3);
UPDATE permitted_prisoner set prison_code = 'NA' where prison_code IS NULL;
ALTER TABLE permitted_prisoner ALTER COLUMN prison_code SET NOT NULL;
