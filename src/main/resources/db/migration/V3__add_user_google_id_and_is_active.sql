-- =============================================================================
-- Migration: V3__add_user_google_id_and_is_active.sql
-- Description: Adiciona campos google_id e is_active na tabela users
-- =============================================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS google_id VARCHAR(100) NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_google_id ON users(google_id) WHERE google_id IS NOT NULL;
