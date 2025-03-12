CREATE TABLE booker_audit
(
    id                SERIAL          NOT NULL PRIMARY KEY,
    booker_reference  VARCHAR(255)    NOT NULL UNIQUE,
    audit_type        VARCHAR(80)     NOT NULL,
    text              TEXT            NOT NULL,
    create_timestamp  TIMESTAMP       default current_timestamp
);

CREATE INDEX idx_booker_reference_booker_audit ON booker_audit(booker_reference);
