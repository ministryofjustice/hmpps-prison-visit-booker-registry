CREATE TABLE auth_detail
(
    id                      SERIAL          NOT NULL PRIMARY KEY,
    one_login_sub           VARCHAR(255)    NOT NULL UNIQUE,
    count                   integer         default 0,
    email                   VARCHAR(100)    NOT NULL,
    phone_number            VARCHAR(40)     NULL,
    create_timestamp        TIMESTAMP       default current_timestamp
);

CREATE INDEX idx_one_login_sub_auth_detail ON auth_detail(one_login_sub);

