CREATE TABLE booker_prisoner
(
    id                      SERIAL          NOT NULL PRIMARY KEY,
    auth_detail_id          integer         NOT NULL,
    prisoner_id             VARCHAR(80)     NOT NULL,
    active                  boolean         NOT NULL,
    create_timestamp        timestamp       default current_timestamp
);
