CREATE TABLE booker_prisoner_prison
(
    id                          SERIAL          NOT NULL PRIMARY KEY,
    booker_prisoner_id          integer         NOT NULL,
    prison_code                 VARCHAR(3)      NOT NULL,
    from_date                   DATE            NOT NULL,
    to_date                     DATE,
    create_timestamp            timestamp       default current_timestamp
);
