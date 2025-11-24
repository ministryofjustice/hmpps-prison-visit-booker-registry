BEGIN;

ALTER TABLE permitted_prisoner
    DROP COLUMN active;

ALTER TABLE permitted_visitor
    DROP COLUMN active;

COMMIT;
