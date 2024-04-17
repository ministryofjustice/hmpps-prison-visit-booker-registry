CREATE TABLE booker
(
    id                      SERIAL          NOT NULL PRIMARY KEY,
    reference               text            UNIQUE,
    one_login_sub           VARCHAR(255)    UNIQUE,
    email                   VARCHAR(100)    NOT NULL UNIQUE,
    create_timestamp        TIMESTAMP       default current_timestamp
);

CREATE INDEX idx_auth_reference_booker ON booker(reference);
CREATE INDEX idx_one_login_sub_booker ON booker(one_login_sub);


