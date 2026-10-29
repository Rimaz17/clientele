-- Existing rows have no real sign-up date, so they get the time this migration
-- runs (see ADR 0007). New rows get the time of their INSERT.
ALTER TABLE customer ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();
CREATE INDEX customer_created_at_idx ON customer (created_at);
