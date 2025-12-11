CREATE INDEX idx_vr_status_prisoner_id ON visitor_requests (status, prisoner_id);

CREATE INDEX idx_pp_prison_code_prisoner_id ON permitted_prisoner (prison_code, prisoner_id);
