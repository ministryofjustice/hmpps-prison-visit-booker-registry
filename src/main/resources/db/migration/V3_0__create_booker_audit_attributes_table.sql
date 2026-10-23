CREATE TABLE booker_audit_attribute (
   id                          SERIAL                   NOT NULL PRIMARY KEY,
   booker_audit_id             INTEGER                  NOT NULL,
   attribute_name              VARCHAR(80)              NOT NULL,
   attribute_value             TEXT                     NOT NULL,
   CONSTRAINT fk_booker_audit FOREIGN KEY (booker_audit_id) REFERENCES booker_audit (id) ON DELETE CASCADE
);
