-- ============================================================================
-- V8__create_customers_and_business_customers.sql
-- Descrição:
--   1. Cria o enum `customer_tier` ('NEW', 'REGULAR', 'RECURRENT', 'VIP', 'WARNING').
--   2. Cria a tabela global `customers` e migra utilizadores existentes.
--   3. Cria a tabela multi-tenant `business_customers` para classificação (VIP, etc.)
--      e notas privadas por salão, com backfill das marcações existentes.
--   4. Redireciona FKs de `appointments.customer_id` e `reviews.customer_id`
--      para `customers(id)` e torna `created_by_id` / `performed_by_id` opcionais.
-- ============================================================================

-- 1. Criar ENUM de classificação do cliente no salão
CREATE TYPE customer_tier AS ENUM (
    'NEW',
    'REGULAR',
    'RECURRENT',
    'VIP',
    'WARNING'
);

-- 2. Criar tabela customers
CREATE TABLE customers (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NULL,
    name        VARCHAR(150) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    phone       VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_customers_email   UNIQUE (email),
    CONSTRAINT uk_customers_user_id UNIQUE (user_id),
    CONSTRAINT fk_customers_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_customers_phone ON customers(phone);

-- 3. Backfill de customers a partir de users existentes
INSERT INTO customers (id, user_id, name, email, phone, created_at, updated_at)
SELECT u.id, u.id, u.name, LOWER(u.email), u.phone, u.created_at, u.updated_at
FROM users u
ON CONFLICT (email) DO NOTHING;

-- 4. Criar tabela multi-tenant business_customers (CRM por salão)
CREATE TABLE business_customers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id     UUID          NOT NULL,
    customer_id     UUID          NOT NULL,
    tier            customer_tier NOT NULL DEFAULT 'NEW',
    internal_notes  TEXT          NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_business_customers_tenant_customer UNIQUE (business_id, customer_id),
    CONSTRAINT fk_business_customers_business_id     FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_business_customers_customer_id     FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX idx_business_customers_business_tier ON business_customers(business_id, tier);

-- 5. Backfill de business_customers para clientes que já têm agendamentos em salões
INSERT INTO business_customers (business_id, customer_id, tier)
SELECT DISTINCT a.business_id, a.customer_id, 'REGULAR'::customer_tier
FROM appointments a
ON CONFLICT (business_id, customer_id) DO NOTHING;

-- 6. Redirecionar FKs de appointments e reviews para customers(id)
ALTER TABLE appointments DROP CONSTRAINT fk_appointments_customer_id;
ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_customer_id
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT;

ALTER TABLE reviews DROP CONSTRAINT fk_reviews_customer;
ALTER TABLE reviews
    ADD CONSTRAINT fk_reviews_customer_id
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT;

-- 7. Tornar created_by_id e performed_by_id opcionais para agendamentos Guest
ALTER TABLE appointments
    ALTER COLUMN created_by_id DROP NOT NULL;

ALTER TABLE appointment_histories
    ALTER COLUMN performed_by_id DROP NOT NULL;
