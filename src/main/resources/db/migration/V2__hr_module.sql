-- ─────────────────────────────────────────────────────────────────────────────
-- HR (İnsan Resursları) modulu — V2
--   - hr_positions             : Vəzifə kataloqu
--   - hr_employees             : İşçilər
--   - hr_tax_rate_configs      : İllik vergi/sığorta tarifləri
--   - hr_payroll_periods       : Aylıq əməkhaqqı dövrləri
--   - hr_payroll_entries       : Hər işçi üzrə aylıq sətir
--   - hr_payroll_adjustments   : Mükafat / cərimə / əlavələr
--   - hr_attendance_records    : Davamiyyət
--   - hr_leave_requests        : Məzuniyyət tələbləri
-- ─────────────────────────────────────────────────────────────────────────────

-- Vəzifə kataloqu
CREATE TABLE hr_positions (
    id               BIGSERIAL PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL,
    deleted          BOOLEAN   NOT NULL DEFAULT false,
    deleted_at       TIMESTAMP,
    name             VARCHAR(255) NOT NULL,
    description      VARCHAR(1000),
    default_salary   NUMERIC(19, 2),
    department_id    BIGINT REFERENCES departments(id),
    active           BOOLEAN   NOT NULL DEFAULT true
);
CREATE INDEX idx_hr_positions_department ON hr_positions(department_id);
CREATE INDEX idx_hr_positions_deleted ON hr_positions(deleted);

-- İşçilər
CREATE TABLE hr_employees (
    id                 BIGSERIAL PRIMARY KEY,
    created_at         TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP NOT NULL,
    deleted            BOOLEAN   NOT NULL DEFAULT false,
    deleted_at         TIMESTAMP,
    employee_code      VARCHAR(50) UNIQUE,
    first_name         VARCHAR(255) NOT NULL,
    last_name          VARCHAR(255) NOT NULL,
    father_name        VARCHAR(255),
    fin                VARCHAR(20) UNIQUE,
    id_card_series     VARCHAR(20),
    id_card_number     VARCHAR(50),
    gender             VARCHAR(20),
    birth_date         DATE,
    phone              VARCHAR(50),
    email              VARCHAR(255),
    address            VARCHAR(500),
    position_id        BIGINT REFERENCES hr_positions(id),
    department_id      BIGINT REFERENCES departments(id),
    gross_salary       NUMERIC(19, 2) NOT NULL,
    hire_date          DATE,
    termination_date   DATE,
    termination_reason VARCHAR(500),
    status             VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    bank_name          VARCHAR(255),
    bank_account       VARCHAR(50),
    photo_url          VARCHAR(500),
    notes              VARCHAR(1000),
    annual_leave_days  INTEGER NOT NULL DEFAULT 21
);
CREATE INDEX idx_hr_employees_status ON hr_employees(status);
CREATE INDEX idx_hr_employees_department ON hr_employees(department_id);
CREATE INDEX idx_hr_employees_position ON hr_employees(position_id);
CREATE INDEX idx_hr_employees_deleted ON hr_employees(deleted);
CREATE INDEX idx_hr_employees_lastname ON hr_employees(last_name);

-- Vergi tarifləri (illik)
CREATE TABLE hr_tax_rate_configs (
    id                              BIGSERIAL PRIMARY KEY,
    created_at                      TIMESTAMP NOT NULL,
    updated_at                      TIMESTAMP NOT NULL,
    deleted                         BOOLEAN   NOT NULL DEFAULT false,
    deleted_at                      TIMESTAMP,
    effective_year                  INTEGER NOT NULL,
    active                          BOOLEAN NOT NULL DEFAULT true,
    employee_pension_threshold      NUMERIC(10, 4) NOT NULL,
    employee_pension_rate_below     NUMERIC(6, 4)  NOT NULL,
    employee_pension_rate_above     NUMERIC(6, 4)  NOT NULL,
    employer_pension_threshold      NUMERIC(10, 4) NOT NULL,
    employer_pension_rate_below     NUMERIC(6, 4)  NOT NULL,
    employer_pension_rate_above     NUMERIC(6, 4)  NOT NULL,
    employee_unemployment_rate      NUMERIC(6, 4)  NOT NULL,
    employer_unemployment_rate      NUMERIC(6, 4)  NOT NULL,
    employee_medical_threshold      NUMERIC(10, 4) NOT NULL,
    employee_medical_rate_below     NUMERIC(6, 4)  NOT NULL,
    employee_medical_rate_above     NUMERIC(6, 4)  NOT NULL,
    employer_medical_threshold      NUMERIC(10, 4) NOT NULL,
    employer_medical_rate_below     NUMERIC(6, 4)  NOT NULL,
    employer_medical_rate_above     NUMERIC(6, 4)  NOT NULL,
    income_tax_threshold            NUMERIC(10, 4) NOT NULL,
    income_tax_rate_below           NUMERIC(6, 4)  NOT NULL,
    income_tax_rate_above           NUMERIC(6, 4)  NOT NULL,
    non_taxable_minimum             NUMERIC(10, 4) NOT NULL DEFAULT 0,
    deduct_social_from_tax_base     BOOLEAN NOT NULL DEFAULT false,
    notes                           VARCHAR(500),
    CONSTRAINT uk_tax_rate_year UNIQUE (effective_year)
);
CREATE INDEX idx_hr_tax_rate_active ON hr_tax_rate_configs(active);

-- Aylıq əməkhaqqı dövrləri
CREATE TABLE hr_payroll_periods (
    id                              BIGSERIAL PRIMARY KEY,
    created_at                      TIMESTAMP NOT NULL,
    updated_at                      TIMESTAMP NOT NULL,
    deleted                         BOOLEAN   NOT NULL DEFAULT false,
    deleted_at                      TIMESTAMP,
    period_year                     INTEGER NOT NULL,
    period_month                    INTEGER NOT NULL,
    working_days_in_month           INTEGER NOT NULL DEFAULT 22,
    working_hours_per_day           INTEGER NOT NULL DEFAULT 8,
    status                          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_gross                     NUMERIC(19, 2) DEFAULT 0,
    total_net                       NUMERIC(19, 2) DEFAULT 0,
    total_employee_deductions       NUMERIC(19, 2) DEFAULT 0,
    total_employer_contributions    NUMERIC(19, 2) DEFAULT 0,
    total_income_tax                NUMERIC(19, 2) DEFAULT 0,
    approved_at                     TIMESTAMP,
    approved_by                     VARCHAR(255),
    paid_at                         TIMESTAMP,
    notes                           VARCHAR(500),
    invoice_id                      BIGINT,
    CONSTRAINT uk_period_year_month UNIQUE (period_year, period_month)
);
CREATE INDEX idx_hr_payroll_periods_status ON hr_payroll_periods(status);

-- Hər işçi üçün aylıq sətir
CREATE TABLE hr_payroll_entries (
    id                              BIGSERIAL PRIMARY KEY,
    created_at                      TIMESTAMP NOT NULL,
    updated_at                      TIMESTAMP NOT NULL,
    deleted                         BOOLEAN   NOT NULL DEFAULT false,
    deleted_at                      TIMESTAMP,
    period_id                       BIGINT NOT NULL REFERENCES hr_payroll_periods(id),
    employee_id                     BIGINT NOT NULL REFERENCES hr_employees(id),
    employee_full_name              VARCHAR(255) NOT NULL,
    position_name                   VARCHAR(255),
    base_salary                     NUMERIC(19, 2) NOT NULL,
    working_days_in_month           INTEGER NOT NULL DEFAULT 22,
    actual_days_worked              INTEGER NOT NULL DEFAULT 22,
    extra_hours                     NUMERIC(19, 2) DEFAULT 0,
    overtime_pay                    NUMERIC(19, 2) DEFAULT 0,
    bonus                           NUMERIC(19, 2) DEFAULT 0,
    vacation_pay                    NUMERIC(19, 2) DEFAULT 0,
    penalty                         NUMERIC(19, 2) DEFAULT 0,
    gross_total                     NUMERIC(19, 2) NOT NULL DEFAULT 0,
    income_tax                      NUMERIC(19, 2) DEFAULT 0,
    employee_pension                NUMERIC(19, 2) DEFAULT 0,
    employee_unemployment           NUMERIC(19, 2) DEFAULT 0,
    employee_medical                NUMERIC(19, 2) DEFAULT 0,
    total_deductions                NUMERIC(19, 2) NOT NULL DEFAULT 0,
    net_pay                         NUMERIC(19, 2) NOT NULL DEFAULT 0,
    employer_pension                NUMERIC(19, 2) DEFAULT 0,
    employer_unemployment           NUMERIC(19, 2) DEFAULT 0,
    employer_medical                NUMERIC(19, 2) DEFAULT 0,
    total_employer_contributions    NUMERIC(19, 2) DEFAULT 0,
    total_company_cost              NUMERIC(19, 2) NOT NULL DEFAULT 0,
    notes                           VARCHAR(500),
    CONSTRAINT uk_entry_period_employee UNIQUE (period_id, employee_id)
);
CREATE INDEX idx_hr_payroll_entries_period ON hr_payroll_entries(period_id);
CREATE INDEX idx_hr_payroll_entries_employee ON hr_payroll_entries(employee_id);
CREATE INDEX idx_hr_payroll_entries_deleted ON hr_payroll_entries(deleted);

-- Mükafat və cərimə
CREATE TABLE hr_payroll_adjustments (
    id          BIGSERIAL PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    deleted     BOOLEAN   NOT NULL DEFAULT false,
    deleted_at  TIMESTAMP,
    entry_id    BIGINT NOT NULL REFERENCES hr_payroll_entries(id),
    type        VARCHAR(30) NOT NULL,
    amount      NUMERIC(19, 2) NOT NULL,
    reason      VARCHAR(500)
);
CREATE INDEX idx_hr_adjustments_entry ON hr_payroll_adjustments(entry_id);

-- Davamiyyət
CREATE TABLE hr_attendance_records (
    id                BIGSERIAL PRIMARY KEY,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL,
    deleted           BOOLEAN   NOT NULL DEFAULT false,
    deleted_at        TIMESTAMP,
    employee_id       BIGINT NOT NULL REFERENCES hr_employees(id),
    attendance_date   DATE NOT NULL,
    status            VARCHAR(30) NOT NULL DEFAULT 'PRESENT',
    hours_worked      NUMERIC(5, 2) DEFAULT 8.00,
    notes             VARCHAR(500),
    CONSTRAINT uk_attendance_employee_date UNIQUE (employee_id, attendance_date)
);
CREATE INDEX idx_hr_attendance_employee ON hr_attendance_records(employee_id);
CREATE INDEX idx_hr_attendance_date ON hr_attendance_records(attendance_date);

-- Məzuniyyət
CREATE TABLE hr_leave_requests (
    id              BIGSERIAL PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    deleted         BOOLEAN   NOT NULL DEFAULT false,
    deleted_at      TIMESTAMP,
    employee_id     BIGINT NOT NULL REFERENCES hr_employees(id),
    type            VARCHAR(30) NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    days            INTEGER NOT NULL,
    reason          VARCHAR(1000),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decided_by      VARCHAR(255),
    decided_at      TIMESTAMP,
    decision_note   VARCHAR(500)
);
CREATE INDEX idx_hr_leaves_employee ON hr_leave_requests(employee_id);
CREATE INDEX idx_hr_leaves_status ON hr_leave_requests(status);
CREATE INDEX idx_hr_leaves_dates ON hr_leave_requests(start_date, end_date);
