CREATE TABLE visitor_requests
(
    id                      SERIAL          NOT NULL PRIMARY KEY,
    reference               text            UNIQUE,
    booker_reference        VARCHAR(255)    NOT NULL,
    prisoner_id             VARCHAR(80)     NOT NULL,
    first_name              VARCHAR(255)    NOT NULL,
    last_name               VARCHAR(255)    NOT NULL,
    date_of_birth           DATE            NOT NULL,
    status                  VARCHAR(50)     NOT NULL,
    create_timestamp        TIMESTAMP       default current_timestamp
);

CREATE INDEX idx_visitor_requests_booker_reference ON visitor_requests(booker_reference);


