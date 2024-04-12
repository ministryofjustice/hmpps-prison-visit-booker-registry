CREATE TABLE booker_prisoner_visitor
(
    id                          SERIAL          NOT NULL PRIMARY KEY,
    booker_prisoner_id          integer         NOT NULL,
    visitor_id                  integer         NOT NULL,
    active                      boolean         NOT NULL,
    create_timestamp            timestamp default current_timestamp,

    CONSTRAINT visitor_to_prisoner  FOREIGN KEY (booker_prisoner_id) REFERENCES booker_prisoner(id)
);
