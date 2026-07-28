-- ============================================
-- V4: Add optimistic locking version column to prescriptions
-- ============================================

ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
