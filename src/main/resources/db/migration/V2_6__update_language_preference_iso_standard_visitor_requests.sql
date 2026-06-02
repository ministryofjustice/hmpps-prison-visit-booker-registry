ALTER TABLE visitor_requests
    ALTER COLUMN language_preference SET DEFAULT 'en';

UPDATE visitor_requests
SET language_preference = LOWER(language_preference)
WHERE language_preference IS NOT NULL;