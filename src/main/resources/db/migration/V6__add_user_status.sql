-- Flyway migration: V6__add_user_status.sql
-- Add status column and index to users table

ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX idx_users_status ON users (status);

