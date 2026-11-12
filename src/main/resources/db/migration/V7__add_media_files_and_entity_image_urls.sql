-- =============================================================================
-- Migration: V7__add_media_files_and_entity_image_urls.sql
-- Description: Catálogo de metadados de ficheiros (media_files) e colunas de
--              foto principal para businesses (logo_url), services (image_url)
--              e employees (avatar_url).
-- =============================================================================

-- 1. Tipo enumerado para o escopo/finalidade do ficheiro carregado
CREATE TYPE media_scope AS ENUM (
    'USER_AVATAR',
    'BUSINESS_LOGO',
    'SERVICE_IMAGE',
    'EMPLOYEE_AVATAR'
);

-- 2. Colunas de imagem principal (1 foto por entidade no MVP)
ALTER TABLE businesses
    ADD COLUMN logo_url TEXT NULL;

ALTER TABLE services
    ADD COLUMN image_url TEXT NULL;

ALTER TABLE employees
    ADD COLUMN avatar_url TEXT NULL;

-- 3. Tabela de metadados de ficheiros (os ficheiros físicos ficam em disco/Object Storage)
CREATE TABLE media_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NULL,
    uploaded_by_id UUID NOT NULL,
    scope media_scope NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_media_files_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_media_files_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_media_files_uploaded_by FOREIGN KEY (uploaded_by_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT ck_media_files_size CHECK (size_bytes > 0 AND size_bytes <= 5242880)
);

CREATE INDEX idx_media_files_business_scope ON media_files(business_id, scope, created_at DESC);
CREATE INDEX idx_media_files_uploaded_by ON media_files(uploaded_by_id, created_at DESC);

