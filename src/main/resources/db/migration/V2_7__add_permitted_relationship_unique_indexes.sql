CREATE UNIQUE INDEX ux_permitted_prisoner_booker_prisoner
ON permitted_prisoner (booker_id, lower(prisoner_id));

CREATE UNIQUE INDEX ux_permitted_visitor_prisoner_visitor
ON permitted_visitor (permitted_prisoner_id, visitor_id);
