-- =============================================================================
-- Migration: V5__add_categories_and_business_categories.sql
-- Description: Tabela de categorias (com seed inicial), relação N:M para espaços
--              mistos (business_categories com is_primary) e category_id em services
-- =============================================================================

-- 1. Criação da tabela de categorias
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(50) NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
);

CREATE INDEX idx_categories_active_order ON categories(is_active, display_order);

-- 2. Seed inicial das categorias oficiais para Cabo Verde
INSERT INTO categories (id, slug, name, icon, display_order, is_active) VALUES
    (gen_random_uuid(), 'barbershop',   'Barbearia',                      'Scissors', 1, TRUE),
    (gen_random_uuid(), 'hair',         'Cabeleireiro & Tranças',         'Sparkles', 2, TRUE),
    (gen_random_uuid(), 'nails',        'Unhas (Manicure & Pedicure)',    'Hand',     3, TRUE),
    (gen_random_uuid(), 'spa_massage',  'Spa & Massagem',                 'Flower2',  4, TRUE),
    (gen_random_uuid(), 'aesthetics',   'Estética & Depilação',           'Wand2',    5, TRUE),
    (gen_random_uuid(), 'makeup_brows', 'Maquilhagem & Sobrancelhas',     'Eye',      6, TRUE)
ON CONFLICT (slug) DO NOTHING;

-- 3. Tabela associativa N:M entre businesses e categories (suporte a espaços mistos)
CREATE TABLE business_categories (
    business_id UUID NOT NULL,
    category_id UUID NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_business_categories PRIMARY KEY (business_id, category_id),
    CONSTRAINT fk_biz_categories_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_biz_categories_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
);

-- Garante que cada negócio tenha no máximo 1 categoria principal (is_primary = TRUE)
CREATE UNIQUE INDEX uk_business_categories_primary
    ON business_categories(business_id)
    WHERE is_primary = TRUE;

CREATE INDEX idx_business_categories_category
    ON business_categories(category_id, business_id);

-- 4. Adição de category_id (1 categoria por serviço) na tabela services
ALTER TABLE services
    ADD COLUMN category_id UUID NULL;

-- Backfill seguro para serviços já existentes em ambiente dev/local (atribui 'hair' por defeito)
UPDATE services
SET category_id = (SELECT id FROM categories WHERE slug = 'hair' LIMIT 1)
WHERE category_id IS NULL;

ALTER TABLE services
    ALTER COLUMN category_id SET NOT NULL,
    ADD CONSTRAINT fk_services_category_id FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT;

CREATE INDEX idx_services_category_active ON services(category_id, is_active);

