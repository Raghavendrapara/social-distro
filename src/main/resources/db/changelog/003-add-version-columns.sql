-- Add version columns for optimistic locking
ALTER TABLE pods ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE indexing_jobs ADD COLUMN version BIGINT DEFAULT 0;
