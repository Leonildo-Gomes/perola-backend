-- =============================================================================
-- Migration: V2__seed_cape_verde_holidays.sql
-- Description: Carga dos feriados nacionais fixos e de referência em Cabo Verde
-- =============================================================================

INSERT INTO holidays (id, holiday_date, name, is_national) VALUES
    (gen_random_uuid(), '2026-01-01', 'Dia de Ano Novo', TRUE),
    (gen_random_uuid(), '2026-01-13', 'Dia da Liberdade e da Democracia', TRUE),
    (gen_random_uuid(), '2026-01-20', 'Dia dos Heróis Nacionais', TRUE),
    (gen_random_uuid(), '2026-02-17', 'Terça-feira de Carnaval', TRUE),
    (gen_random_uuid(), '2026-04-03', 'Sexta-feira Santa', TRUE),
    (gen_random_uuid(), '2026-04-05', 'Páscoa', TRUE),
    (gen_random_uuid(), '2026-05-01', 'Dia do Trabalhador', TRUE),
    (gen_random_uuid(), '2026-06-01', 'Dia Internacional da Criança', TRUE),
    (gen_random_uuid(), '2026-07-05', 'Dia da Independência Nacional', TRUE),
    (gen_random_uuid(), '2026-08-15', 'Dia de Nossa Senhora da Graça / Assunção', TRUE),
    (gen_random_uuid(), '2026-11-01', 'Dia de Todos os Santos', TRUE),
    (gen_random_uuid(), '2026-12-25', 'Dia de Natal', TRUE)
ON CONFLICT (holiday_date) DO NOTHING;
