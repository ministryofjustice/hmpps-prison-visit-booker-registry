CREATE TABLE auth_detail
(
    id                      SERIAL          NOT NULL PRIMARY KEY,
    auth_reference          VARCHAR(255)    NOT NULL UNIQUE,
    auth_email              VARCHAR(100)    NOT NULL,
    auth_phone_number       VARCHAR(40)     NULL,
    create_timestamp        TIMESTAMP       default current_timestamp
);

CREATE INDEX idx_auth_reference_auth_detail ON auth_detail(auth_reference);

