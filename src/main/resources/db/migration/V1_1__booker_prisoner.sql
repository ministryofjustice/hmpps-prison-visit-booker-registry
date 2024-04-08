CREATE TABLE booker_prisoner
(
    id                      SERIAL          NOT NULL PRIMARY KEY,
    booker_id               integer         NOT NULL,
    prison_number           VARCHAR(80)     NOT NULL,
    active                  boolean         NOT NULL,
    create_timestamp        timestamp       default current_timestamp,

    CONSTRAINT prisoner_to_booker  FOREIGN KEY (booker_id) REFERENCES auth_detail(id)
);
