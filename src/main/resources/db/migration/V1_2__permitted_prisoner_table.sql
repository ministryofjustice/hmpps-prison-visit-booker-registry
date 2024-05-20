CREATE TABLE permitted_prisoner
(
    id                      SERIAL          NOT NULL PRIMARY KEY,
    booker_id               integer         NOT NULL,
    prisoner_id             VARCHAR(80)     NOT NULL,
    active                  boolean         NOT NULL,
    create_timestamp        timestamp       default current_timestamp,

    CONSTRAINT permitted_prisoner_to_booker  FOREIGN KEY (booker_id) REFERENCES booker(id)
);
