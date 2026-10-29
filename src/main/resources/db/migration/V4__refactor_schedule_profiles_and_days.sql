-- =============================================================================
-- Migration: V4__refactor_schedule_profiles_and_days.sql
-- Description: Refatora employee_schedules para perfis/templates de horário,
--              cria employee_schedule_days, vincula employee_schedule_breaks ao dia,
--              e adiciona employee_schedule_id em employee_monthly_schedules.
-- =============================================================================

-- 1. Limpar dados transitórios das tabelas de horário anteriores para reestruturação limpa
DELETE FROM employee_monthly_schedules;
DELETE FROM employee_schedule_breaks;
DELETE FROM employee_schedules;

-- 2. Reestruturar employee_schedules como cabeçalho/perfil de horário semanal
ALTER TABLE employee_schedules
    DROP CONSTRAINT IF EXISTS uk_employee_schedules_day,
    DROP CONSTRAINT IF EXISTS ck_employee_schedules_time;

DROP INDEX IF EXISTS idx_employee_schedules_day;

ALTER TABLE employee_schedules
    DROP COLUMN day_of_week,
    DROP COLUMN start_time,
    DROP COLUMN end_time,
    ADD COLUMN name VARCHAR(100) NOT NULL DEFAULT 'Horário Padrão',
    ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_employee_schedules_employee ON employee_schedules(employee_id, is_active);
CREATE UNIQUE INDEX uk_employee_schedules_default ON employee_schedules(employee_id)
    WHERE is_default = TRUE AND is_active = TRUE;

-- 3. Criar tabela employee_schedule_days (os 7 dias de cada perfil de horário)
CREATE TABLE employee_schedule_days (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_schedule_id UUID NOT NULL,
    day_of_week day_of_week NOT NULL,
    is_working BOOLEAN NOT NULL DEFAULT TRUE,
    start_time TIME NULL,
    end_time TIME NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_emp_schedule_days_schedule FOREIGN KEY (employee_schedule_id) REFERENCES employee_schedules(id) ON DELETE CASCADE,
    CONSTRAINT uk_emp_schedule_days_weekday UNIQUE (employee_schedule_id, day_of_week),
    CONSTRAINT ck_emp_schedule_days_working_times CHECK (
        (is_working = FALSE AND start_time IS NULL AND end_time IS NULL) OR
        (is_working = TRUE AND start_time IS NOT NULL AND end_time IS NOT NULL AND end_time > start_time)
    )
);

CREATE INDEX idx_emp_schedule_days_lookup ON employee_schedule_days(employee_schedule_id, day_of_week, is_working);

-- 4. Atualizar employee_schedule_breaks para apontar para employee_schedule_days
DROP INDEX IF EXISTS idx_emp_breaks_schedule_id;

ALTER TABLE employee_schedule_breaks
    DROP CONSTRAINT IF EXISTS fk_emp_breaks_schedule,
    DROP COLUMN employee_schedule_id,
    ADD COLUMN employee_schedule_day_id UUID NOT NULL;

ALTER TABLE employee_schedule_breaks
    ADD CONSTRAINT fk_emp_breaks_schedule_day FOREIGN KEY (employee_schedule_day_id) REFERENCES employee_schedule_days(id) ON DELETE CASCADE;

CREATE INDEX idx_emp_breaks_schedule_day_id ON employee_schedule_breaks(employee_schedule_day_id);

-- 5. Vincular employee_monthly_schedules ao perfil employee_schedules escolhido
ALTER TABLE employee_monthly_schedules
    ADD COLUMN employee_schedule_id UUID NOT NULL,
    ADD CONSTRAINT fk_emp_monthly_schedule FOREIGN KEY (employee_schedule_id) REFERENCES employee_schedules(id) ON DELETE RESTRICT;

CREATE INDEX idx_emp_monthly_schedule_id ON employee_monthly_schedules(employee_schedule_id);

