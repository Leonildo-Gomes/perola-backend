-- =============================================================================
-- Migration: V1__initial_schema.sql
-- Description: Criação das extensões, tipos enum, tabelas, restrições e índices do Perola MVP
-- =============================================================================

-- 1. EXTENSÕES
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "btree_gist";

-- =============================================================================
-- 2. TIPOS PERSONALIZADOS (ENUMS)
-- =============================================================================

CREATE TYPE user_role AS ENUM (
    'CUSTOMER',
    'OWNER',
    'EMPLOYEE'
);

CREATE TYPE day_of_week AS ENUM (
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY',
    'SUNDAY'
);

CREATE TYPE time_off_type AS ENUM (
    'VACATION',
    'SICK_LEAVE',
    'PERSONAL',
    'HOLIDAY',
    'OTHER'
);

CREATE TYPE appointment_status AS ENUM (
    'PENDING',
    'CONFIRMED',
    'RESCHEDULE_PENDING',
    'CANCELLED',
    'COMPLETED'
);

CREATE TYPE appointment_action AS ENUM (
    'CREATED',
    'CONFIRMED',
    'RESCHEDULE_REQUESTED',
    'RESCHEDULE_CONFIRMED',
    'RESCHEDULE_REJECTED',
    'CANCEL_REQUESTED',
    'CANCELLED',
    'COMPLETED'
);

-- =============================================================================
-- 3. MÓDULO IDENTITY
-- =============================================================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NULL,
    avatar_url TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);

CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    role user_role NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_refresh_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_active ON refresh_tokens(user_id, expires_at) WHERE revoked_at IS NULL;

-- =============================================================================
-- 4. MÓDULO BUSINESS
-- =============================================================================

CREATE TABLE businesses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    island VARCHAR(50) NOT NULL,
    city VARCHAR(100) NOT NULL,
    location TEXT NOT NULL,
    contact_phone VARCHAR(50) NOT NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Atlantic/Cape_Verde',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_businesses_owner_id FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT uk_businesses_owner_id UNIQUE (owner_id)
);

CREATE INDEX idx_businesses_owner_id ON businesses(owner_id);
CREATE INDEX idx_businesses_active_search ON businesses(island, city, name) WHERE is_active = TRUE;

CREATE TABLE services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    duration_minutes INTEGER NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_services_business_id FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE RESTRICT,
    CONSTRAINT ck_services_duration_minutes CHECK (duration_minutes > 0),
    CONSTRAINT ck_services_price CHECK (price >= 0)
);

CREATE INDEX idx_services_business_active ON services(business_id, is_active);

CREATE TABLE employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    user_id UUID NULL,
    display_name VARCHAR(150) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_employees_business_id FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_employees_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT uk_employees_user_id UNIQUE (user_id)
);

CREATE INDEX idx_employees_business_active ON employees(business_id, is_active);

CREATE TABLE employee_services (
    business_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    service_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_employee_services PRIMARY KEY (employee_id, service_id),
    CONSTRAINT fk_emp_services_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_emp_services_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_emp_services_service FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE RESTRICT
);

CREATE INDEX idx_employee_services_business ON employee_services(business_id);
CREATE INDEX idx_employee_services_service_id ON employee_services(service_id);

-- =============================================================================
-- 5. MÓDULO SCHEDULE
-- =============================================================================

CREATE TABLE employee_monthly_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    is_opened BOOLEAN NOT NULL DEFAULT TRUE,
    opened_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_emp_monthly_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT uk_emp_monthly_schedule UNIQUE (employee_id, year, month),
    CONSTRAINT ck_emp_monthly_month CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT ck_emp_monthly_year CHECK (year >= 2024)
);

CREATE INDEX idx_emp_monthly_lookup ON employee_monthly_schedules(employee_id, year, month, is_opened);

CREATE TABLE employee_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL,
    day_of_week day_of_week NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_emp_schedules_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT uk_employee_schedules_day UNIQUE (employee_id, day_of_week),
    CONSTRAINT ck_employee_schedules_time CHECK (end_time > start_time)
);

CREATE INDEX idx_employee_schedules_day ON employee_schedules(employee_id, day_of_week, is_active);

CREATE TABLE employee_schedule_breaks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_schedule_id UUID NOT NULL,
    title VARCHAR(100) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_emp_breaks_schedule FOREIGN KEY (employee_schedule_id) REFERENCES employee_schedules(id) ON DELETE CASCADE,
    CONSTRAINT ck_emp_breaks_time CHECK (end_time > start_time)
);

CREATE INDEX idx_emp_breaks_schedule_id ON employee_schedule_breaks(employee_schedule_id);

CREATE TABLE employee_time_off (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_all_day BOOLEAN NOT NULL DEFAULT TRUE,
    start_time TIME NULL,
    end_time TIME NULL,
    type time_off_type NOT NULL,
    note TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_emp_time_off_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT ck_emp_time_off_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_emp_time_off_partial_times CHECK (
        is_all_day = TRUE OR (start_time IS NOT NULL AND end_time IS NOT NULL AND end_time > start_time)
    )
);

CREATE INDEX idx_emp_time_off_lookup ON employee_time_off(employee_id, start_date, end_date);

CREATE TABLE holidays (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    holiday_date DATE NOT NULL,
    name VARCHAR(150) NOT NULL,
    is_national BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_holidays_date UNIQUE (holiday_date)
);

CREATE INDEX idx_holidays_date ON holidays(holiday_date);

-- =============================================================================
-- 6. MÓDULO BOOKING
-- =============================================================================

CREATE TABLE appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    service_id UUID NOT NULL,
    created_by_id UUID NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    proposed_start_time TIMESTAMPTZ NULL,
    proposed_end_time TIMESTAMPTZ NULL,
    service_name VARCHAR(150) NOT NULL,
    service_price NUMERIC(10, 2) NOT NULL,
    service_duration INTEGER NOT NULL,
    status appointment_status NOT NULL DEFAULT 'PENDING',
    notes TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_appointments_business_id FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_customer_id FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_employee_id FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_service_id FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_created_by_id FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT ck_appointments_times CHECK (end_time > start_time),
    CONSTRAINT ck_appointments_proposed_times CHECK (
        (proposed_start_time IS NULL AND proposed_end_time IS NULL) OR 
        (proposed_start_time IS NOT NULL AND proposed_end_time IS NOT NULL AND proposed_end_time > proposed_start_time)
    ),
    CONSTRAINT ck_appointments_price CHECK (service_price >= 0),
    CONSTRAINT ck_appointments_duration CHECK (service_duration > 0),
    CONSTRAINT ex_appointments_no_overlap EXCLUDE USING gist (
        employee_id WITH =,
        tstzrange(start_time, end_time, '[)') WITH &&
    ) WHERE (status IN ('PENDING', 'CONFIRMED', 'RESCHEDULE_PENDING'))
);

CREATE INDEX idx_appointments_business_status ON appointments(business_id, status, start_time);
CREATE INDEX idx_appointments_customer_status ON appointments(customer_id, status, start_time DESC);
CREATE INDEX idx_appointments_slot_calc ON appointments(employee_id, start_time, end_time) 
    WHERE status IN ('PENDING', 'CONFIRMED', 'RESCHEDULE_PENDING');

CREATE TABLE appointment_histories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL,
    performed_by_id UUID NOT NULL,
    action appointment_action NOT NULL,
    from_status appointment_status NULL,
    to_status appointment_status NOT NULL,
    old_start_time TIMESTAMPTZ NULL,
    old_end_time TIMESTAMPTZ NULL,
    new_start_time TIMESTAMPTZ NULL,
    new_end_time TIMESTAMPTZ NULL,
    notes TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_appt_hist_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    CONSTRAINT fk_appt_hist_performed_by FOREIGN KEY (performed_by_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX idx_appt_hist_timeline ON appointment_histories(appointment_id, created_at ASC);

CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    rating INTEGER NOT NULL,
    comment TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_reviews_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE RESTRICT,
    CONSTRAINT uk_reviews_appointment UNIQUE (appointment_id),
    CONSTRAINT ck_reviews_rating_range CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_reviews_employee_rating ON reviews(employee_id, rating);
