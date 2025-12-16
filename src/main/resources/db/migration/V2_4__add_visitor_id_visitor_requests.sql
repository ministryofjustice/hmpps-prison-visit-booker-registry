alter table visitor_requests ADD visitor_id integer DEFAULT NULL;
alter table visitor_requests ADD rejection_reason VARCHAR(20) DEFAULT NULL;
