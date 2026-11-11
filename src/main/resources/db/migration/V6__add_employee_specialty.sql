-- =============================================================================
-- Migration: V6__add_employee_specialty.sql
-- Description: Adição do campo specialty na tabela employees para exibir o
--              título/especialidade no perfil do profissional
-- =============================================================================

ALTER TABLE employees
    ADD COLUMN specialty VARCHAR(120) NULL;

