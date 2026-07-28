-- ============================================
-- V2: Add optimistic locking version columns
-- ============================================

ALTER TABLE medicines ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE reminders ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE commands ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;