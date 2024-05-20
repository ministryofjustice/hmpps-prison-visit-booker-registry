CREATE TABLE permitted_visitor
(
    id                          SERIAL          NOT NULL PRIMARY KEY,
    permitted_prisoner_id       integer         NOT NULL,
    visitor_id                  integer         NOT NULL,
    active                      boolean         NOT NULL,
    create_timestamp            timestamp default current_timestamp,

    CONSTRAINT permitted_visitor_to_permitted_prisoner  FOREIGN KEY (permitted_prisoner_id) REFERENCES permitted_prisoner(id)
);
