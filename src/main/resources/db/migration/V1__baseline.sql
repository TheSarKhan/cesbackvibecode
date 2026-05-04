-- V1 Baseline: CES ERP tam sxema (Hibernate ddl-auto:update-dan Flyway-a keçid)
-- Mövcud verilənlər bazasında bu fayl icra edilmir (baseline-on-migrate=true).
-- Yeni mühitdə bütün cədvəllər bu fayl vasitəsilə yaradılır.

-- ─── Əsas lüğət cədvəlləri ────────────────────────────────────────────────

CREATE TABLE departments (
    id               BIGSERIAL PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL,
    deleted          BOOLEAN   NOT NULL DEFAULT false,
    deleted_at       TIMESTAMP,
    name             VARCHAR(255) NOT NULL UNIQUE,
    description      VARCHAR(255),
    active           BOOLEAN   NOT NULL DEFAULT true
);

CREATE TABLE system_modules (
    id          BIGSERIAL PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    deleted     BOOLEAN   NOT NULL DEFAULT false,
    deleted_at  TIMESTAMP,
    code        VARCHAR(255) NOT NULL UNIQUE,
    name_az     VARCHAR(255) NOT NULL,
    name_en     VARCHAR(255) NOT NULL,
    order_index INTEGER      NOT NULL
);

CREATE TABLE config_items (
    id          BIGSERIAL PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    deleted     BOOLEAN   NOT NULL DEFAULT false,
    deleted_at  TIMESTAMP,
    category    VARCHAR(100) NOT NULL,
    item_key    VARCHAR(200) NOT NULL,
    value       VARCHAR(500),
    description VARCHAR(1000),
    sort_order  INTEGER  NOT NULL DEFAULT 0,
    active      BOOLEAN  NOT NULL DEFAULT true,
    CONSTRAINT uq_config_category_key UNIQUE (category, item_key)
);

CREATE INDEX idx_config_category ON config_items (category);
CREATE INDEX idx_config_deleted   ON config_items (deleted);

-- ─── Rol / İstifadəçi ─────────────────────────────────────────────────────

CREATE TABLE roles (
    id            BIGSERIAL PRIMARY KEY,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    deleted       BOOLEAN   NOT NULL DEFAULT false,
    deleted_at    TIMESTAMP,
    name          VARCHAR(255) NOT NULL,
    description   VARCHAR(255),
    department_id BIGINT NOT NULL REFERENCES departments (id),
    active        BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE role_approval_departments (
    role_id       BIGINT NOT NULL REFERENCES roles (id),
    department_id BIGINT NOT NULL REFERENCES departments (id),
    PRIMARY KEY (role_id, department_id)
);

CREATE TABLE role_permissions (
    id                     BIGSERIAL PRIMARY KEY,
    role_id                BIGINT  NOT NULL REFERENCES roles (id),
    module_id              BIGINT  NOT NULL REFERENCES system_modules (id),
    can_get                BOOLEAN NOT NULL DEFAULT false,
    can_post               BOOLEAN NOT NULL DEFAULT false,
    can_put                BOOLEAN NOT NULL DEFAULT false,
    can_delete             BOOLEAN NOT NULL DEFAULT false,
    can_send_to_coordinator BOOLEAN NOT NULL DEFAULT false,
    can_submit_offer       BOOLEAN NOT NULL DEFAULT false,
    can_send_to_accounting BOOLEAN NOT NULL DEFAULT false,
    can_return_to_project  BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uq_role_module UNIQUE (role_id, module_id)
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    deleted       BOOLEAN   NOT NULL DEFAULT false,
    deleted_at    TIMESTAMP,
    full_name     VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    phone         VARCHAR(255),
    department_id BIGINT REFERENCES departments (id),
    role_id       BIGINT REFERENCES roles (id),
    active        BOOLEAN   NOT NULL DEFAULT true,
    has_approval  BOOLEAN   NOT NULL DEFAULT false,
    last_login_at TIMESTAMP
);

CREATE TABLE user_approval_departments (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users (id),
    department_id BIGINT NOT NULL REFERENCES departments (id),
    CONSTRAINT uq_user_approval_dept UNIQUE (user_id, department_id)
);

-- ─── Müştərilər ───────────────────────────────────────────────────────────

CREATE TABLE customers (
    id                    BIGSERIAL PRIMARY KEY,
    created_at            TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP NOT NULL,
    deleted               BOOLEAN   NOT NULL DEFAULT false,
    deleted_at            TIMESTAMP,
    company_name          VARCHAR(255) NOT NULL,
    voen                  VARCHAR(20),
    address               TEXT,
    director_name         VARCHAR(150),
    supplier_person       VARCHAR(255),
    supplier_phone        VARCHAR(50),
    office_contact_person VARCHAR(255),
    office_contact_phone  VARCHAR(50),
    status                VARCHAR(50) NOT NULL,
    risk_level            VARCHAR(50) NOT NULL,
    notes                 TEXT
);

CREATE TABLE customer_payment_types (
    customer_id  BIGINT      NOT NULL REFERENCES customers (id),
    payment_type VARCHAR(255)
);

CREATE TABLE customer_documents (
    id            BIGSERIAL PRIMARY KEY,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    deleted       BOOLEAN   NOT NULL DEFAULT false,
    deleted_at    TIMESTAMP,
    customer_id   BIGINT       NOT NULL REFERENCES customers (id),
    file_path     VARCHAR(255) NOT NULL,
    document_name VARCHAR(255),
    file_type     VARCHAR(50),
    document_date DATE,
    uploaded_by   BIGINT REFERENCES users (id)
);

-- ─── Podratçılar / İnvestorlar ────────────────────────────────────────────

CREATE TABLE contractors (
    id           BIGSERIAL PRIMARY KEY,
    created_at   TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP NOT NULL,
    deleted      BOOLEAN   NOT NULL DEFAULT false,
    deleted_at   TIMESTAMP,
    company_name VARCHAR(255) NOT NULL,
    voen         VARCHAR(20)  NOT NULL UNIQUE,
    contact_person VARCHAR(255),
    phone        VARCHAR(50),
    address      TEXT,
    payment_type VARCHAR(50),
    status       VARCHAR(50) NOT NULL,
    rating       NUMERIC(3, 2) DEFAULT 0,
    risk_level   VARCHAR(50) NOT NULL,
    notes        TEXT
);

CREATE TABLE investors (
    id             BIGSERIAL PRIMARY KEY,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL,
    deleted        BOOLEAN   NOT NULL DEFAULT false,
    deleted_at     TIMESTAMP,
    company_name   VARCHAR(255) NOT NULL,
    voen           VARCHAR(20)  NOT NULL UNIQUE,
    contact_person VARCHAR(255),
    contact_phone  VARCHAR(50),
    address        TEXT,
    payment_type   VARCHAR(50),
    status         VARCHAR(50) NOT NULL,
    rating         NUMERIC(3, 2) DEFAULT 0,
    risk_level     VARCHAR(50) NOT NULL,
    notes          TEXT
);

-- ─── Operatorlar ──────────────────────────────────────────────────────────

CREATE TABLE operators (
    id             BIGSERIAL PRIMARY KEY,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL,
    deleted        BOOLEAN   NOT NULL DEFAULT false,
    deleted_at     TIMESTAMP,
    first_name     VARCHAR(255) NOT NULL,
    last_name      VARCHAR(255) NOT NULL,
    address        VARCHAR(255),
    phone          VARCHAR(255),
    email          VARCHAR(255),
    specialization VARCHAR(255),
    notes          TEXT
);

CREATE TABLE operator_documents (
    id            BIGSERIAL PRIMARY KEY,
    operator_id   BIGINT      NOT NULL REFERENCES operators (id),
    document_type VARCHAR(50) NOT NULL,
    file_path     VARCHAR(255) NOT NULL,
    file_name     VARCHAR(255),
    uploaded_at   TIMESTAMP   NOT NULL
);

-- ─── Banklar ──────────────────────────────────────────────────────────────

CREATE TABLE banks (
    id                   BIGSERIAL PRIMARY KEY,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP NOT NULL,
    deleted              BOOLEAN   NOT NULL DEFAULT false,
    deleted_at           TIMESTAMP,
    bank_name            VARCHAR(200) NOT NULL,
    bank_code            VARCHAR(100),
    swift                VARCHAR(50),
    iban                 VARCHAR(100),
    correspondent_account VARCHAR(100),
    settlement_account   VARCHAR(100)
);

-- ─── Texnika (Qaraj) ──────────────────────────────────────────────────────

CREATE TABLE equipment (
    id                        BIGSERIAL PRIMARY KEY,
    created_at                TIMESTAMP NOT NULL,
    updated_at                TIMESTAMP NOT NULL,
    deleted                   BOOLEAN   NOT NULL DEFAULT false,
    deleted_at                TIMESTAMP,
    equipment_code            VARCHAR(50)  NOT NULL UNIQUE,
    name                      VARCHAR(255) NOT NULL,
    type                      VARCHAR(100) NOT NULL,
    serial_number             VARCHAR(100) UNIQUE,
    brand                     VARCHAR(100),
    model                     VARCHAR(100),
    manufacture_year          INTEGER,
    purchase_date             DATE,
    purchase_price            NUMERIC(15, 2),
    plate_number              VARCHAR(50),
    weight_ton                NUMERIC(10, 2),
    current_market_value      NUMERIC(15, 2),
    depreciation_rate         NUMERIC(5, 2),
    hour_km_counter           NUMERIC(12, 2),
    moto_hours                NUMERIC(12, 2),
    storage_location          VARCHAR(255),
    responsible_user_id       BIGINT REFERENCES users (id),
    ownership_type            VARCHAR(50) NOT NULL,
    owner_contractor_id       BIGINT REFERENCES contractors (id),
    owner_investor_name       VARCHAR(255),
    owner_investor_voen       VARCHAR(20),
    owner_investor_phone      VARCHAR(50),
    last_inspection_date      DATE,
    next_inspection_date      DATE,
    technical_readiness_status VARCHAR(100),
    status                    VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    repair_status             VARCHAR(255),
    notes                     TEXT
);

CREATE INDEX idx_equipment_status           ON equipment (status);
CREATE INDEX idx_equipment_ownership_type   ON equipment (ownership_type);
CREATE INDEX idx_equipment_owner_contractor ON equipment (owner_contractor_id);
CREATE INDEX idx_equipment_investor_voen    ON equipment (owner_investor_voen);
CREATE INDEX idx_equipment_deleted          ON equipment (deleted);
CREATE INDEX idx_equipment_type             ON equipment (type);

CREATE TABLE equipment_safety_items (
    equipment_id   BIGINT NOT NULL REFERENCES equipment (id),
    config_item_id BIGINT NOT NULL REFERENCES config_items (id),
    PRIMARY KEY (equipment_id, config_item_id)
);

CREATE TABLE equipment_documents (
    id                  BIGSERIAL PRIMARY KEY,
    equipment_id        BIGINT       NOT NULL REFERENCES equipment (id),
    document_name       VARCHAR(255) NOT NULL,
    document_type       VARCHAR(255),
    file_path           VARCHAR(255) NOT NULL,
    file_type           VARCHAR(255),
    uploaded_by_user_id BIGINT REFERENCES users (id),
    created_at          TIMESTAMP    NOT NULL
);

CREATE TABLE equipment_status_log (
    id           BIGSERIAL PRIMARY KEY,
    equipment_id BIGINT      NOT NULL REFERENCES equipment (id),
    old_status   VARCHAR(50) NOT NULL,
    new_status   VARCHAR(50) NOT NULL,
    reason       TEXT,
    changed_by_id BIGINT REFERENCES users (id),
    changed_at   TIMESTAMP   NOT NULL
);

CREATE TABLE equipment_inspections (
    id                    BIGSERIAL PRIMARY KEY,
    equipment_id          BIGINT NOT NULL REFERENCES equipment (id),
    inspection_date       DATE   NOT NULL,
    description           TEXT,
    performed_by_user_id  BIGINT REFERENCES users (id),
    document_path         VARCHAR(255),
    document_name         VARCHAR(255),
    created_at            TIMESTAMP NOT NULL
);

CREATE TABLE equipment_project_history (
    id                 BIGSERIAL PRIMARY KEY,
    equipment_id       BIGINT NOT NULL REFERENCES equipment (id),
    project_id         BIGINT,
    project_name       VARCHAR(255),
    start_date         DATE,
    end_date           DATE,
    contractor_revenue NUMERIC(15, 2),
    status             VARCHAR(255),
    notes              TEXT,
    created_at         TIMESTAMP NOT NULL
);

CREATE TABLE equipment_images (
    id           BIGSERIAL PRIMARY KEY,
    created_at   TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP NOT NULL,
    deleted      BOOLEAN   NOT NULL DEFAULT false,
    deleted_at   TIMESTAMP,
    equipment_id BIGINT       NOT NULL REFERENCES equipment (id),
    image_path   VARCHAR(255) NOT NULL,
    image_name   VARCHAR(255),
    file_type    VARCHAR(50),
    uploaded_by  BIGINT REFERENCES users (id)
);

-- ─── Sorğular ─────────────────────────────────────────────────────────────

CREATE TABLE tech_requests (
    id                      BIGSERIAL PRIMARY KEY,
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP NOT NULL,
    deleted                 BOOLEAN   NOT NULL DEFAULT false,
    deleted_at              TIMESTAMP,
    status                  VARCHAR(50) NOT NULL,
    request_code            VARCHAR(20) UNIQUE,
    customer_id             BIGINT REFERENCES customers (id),
    company_name            VARCHAR(255) NOT NULL,
    contact_person          VARCHAR(255),
    contact_phone           VARCHAR(50),
    project_name            VARCHAR(255),
    region                  VARCHAR(255),
    request_date            DATE,
    project_type            VARCHAR(20),
    day_count               INTEGER,
    transportation_required BOOLEAN NOT NULL DEFAULT false,
    equipment_id            BIGINT REFERENCES equipment (id),
    created_by_id           BIGINT REFERENCES users (id),
    notes                   TEXT
);

CREATE INDEX idx_techrequest_status     ON tech_requests (status);
CREATE INDEX idx_techrequest_deleted    ON tech_requests (deleted);
CREATE INDEX idx_techrequest_customer   ON tech_requests (customer_id);
CREATE INDEX idx_techrequest_region     ON tech_requests (region);
CREATE INDEX idx_techrequest_created_by ON tech_requests (created_by_id);

CREATE TABLE tech_request_params (
    request_id  BIGINT       NOT NULL REFERENCES tech_requests (id),
    param_key   VARCHAR(100),
    param_value VARCHAR(255)
);

CREATE TABLE request_status_logs (
    id         BIGSERIAL PRIMARY KEY,
    request_id BIGINT      NOT NULL,
    old_status VARCHAR(50) NOT NULL,
    new_status VARCHAR(50) NOT NULL,
    reason     VARCHAR(255),
    changed_by VARCHAR(255),
    changed_at TIMESTAMP
);

-- ─── Koordinator Planları ─────────────────────────────────────────────────

CREATE TABLE coordinator_plans (
    id                    BIGSERIAL PRIMARY KEY,
    created_at            TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP NOT NULL,
    deleted               BOOLEAN   NOT NULL DEFAULT false,
    deleted_at            TIMESTAMP,
    request_id            BIGINT UNIQUE NOT NULL REFERENCES tech_requests (id),
    equipment_id          BIGINT REFERENCES equipment (id),
    operator_id           BIGINT REFERENCES operators (id),
    day_count             INTEGER,
    equipment_price       NUMERIC(12, 2),
    contractor_daily_rate NUMERIC(12, 2),
    contractor_payment    NUMERIC(12, 2),
    operator_payment      NUMERIC(12, 2),
    transportation_price  NUMERIC(12, 2),
    start_date            DATE,
    end_date              DATE,
    notes                 TEXT
);

CREATE TABLE coordinator_plan_safety_items (
    plan_id        BIGINT NOT NULL REFERENCES coordinator_plans (id),
    config_item_id BIGINT NOT NULL REFERENCES config_items (id),
    PRIMARY KEY (plan_id, config_item_id)
);

CREATE TABLE coordinator_documents (
    id            BIGSERIAL PRIMARY KEY,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    deleted       BOOLEAN   NOT NULL DEFAULT false,
    deleted_at    TIMESTAMP,
    plan_id       BIGINT       NOT NULL REFERENCES coordinator_plans (id),
    document_name VARCHAR(255),
    file_path     VARCHAR(255) NOT NULL,
    file_type     VARCHAR(20),
    document_type VARCHAR(50),
    uploaded_by_id BIGINT REFERENCES users (id)
);

-- ─── Layihələr ────────────────────────────────────────────────────────────

CREATE TABLE projects (
    id                 BIGSERIAL PRIMARY KEY,
    created_at         TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP NOT NULL,
    deleted            BOOLEAN   NOT NULL DEFAULT false,
    deleted_at         TIMESTAMP,
    project_code       VARCHAR(20) UNIQUE,
    request_id         BIGINT UNIQUE NOT NULL REFERENCES tech_requests (id),
    status             VARCHAR(50)  NOT NULL,
    start_date         DATE,
    end_date           DATE,
    has_contract       BOOLEAN      NOT NULL DEFAULT false,
    contract_file_path VARCHAR(255),
    contract_file_name VARCHAR(255),
    evacuation_cost    NUMERIC(12, 2),
    scheduled_hours    NUMERIC(8, 2),
    actual_hours       NUMERIC(8, 2),
    overtime_hours     NUMERIC(8, 2),
    overtime_rate      NUMERIC(5, 2),
    overtime_pay       NUMERIC(12, 2)
);

CREATE TABLE project_expenses (
    id         BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP     NOT NULL,
    updated_at TIMESTAMP     NOT NULL,
    deleted    BOOLEAN       NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    project_id BIGINT        NOT NULL REFERENCES projects (id),
    key        VARCHAR(255)  NOT NULL,
    value      NUMERIC(12, 2) NOT NULL,
    date       DATE          NOT NULL
);

CREATE TABLE project_revenues (
    id         BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP     NOT NULL,
    updated_at TIMESTAMP     NOT NULL,
    deleted    BOOLEAN       NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    project_id BIGINT        NOT NULL REFERENCES projects (id),
    key        VARCHAR(255)  NOT NULL,
    value      NUMERIC(12, 2) NOT NULL,
    date       DATE          NOT NULL
);

CREATE TABLE project_payment_entries (
    id           BIGSERIAL PRIMARY KEY,
    created_at   TIMESTAMP     NOT NULL,
    updated_at   TIMESTAMP     NOT NULL,
    deleted      BOOLEAN       NOT NULL DEFAULT false,
    deleted_at   TIMESTAMP,
    project_id   BIGINT        NOT NULL REFERENCES projects (id),
    amount       NUMERIC(12, 2) NOT NULL,
    payment_date DATE          NOT NULL,
    note         VARCHAR(500),
    closed       BOOLEAN       NOT NULL DEFAULT false
);

-- ─── Qaimələr (Mühasibatlıq) ──────────────────────────────────────────────

CREATE TABLE invoices (
    id                    BIGSERIAL PRIMARY KEY,
    created_at            TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP NOT NULL,
    deleted               BOOLEAN   NOT NULL DEFAULT false,
    deleted_at            TIMESTAMP,
    type                  VARCHAR(50)   NOT NULL,
    status                VARCHAR(50)   NOT NULL DEFAULT 'DRAFT',
    accounting_id         VARCHAR(20)   UNIQUE,
    invoice_number        VARCHAR(50),
    amount                NUMERIC(12, 2) NOT NULL,
    invoice_date          DATE          NOT NULL,
    etaxes_id             VARCHAR(255),
    equipment_name        VARCHAR(255),
    company_name          VARCHAR(255),
    service_description   TEXT,
    project_id            BIGINT REFERENCES projects (id),
    contractor_id         BIGINT REFERENCES contractors (id),
    investor_id           BIGINT REFERENCES investors (id),
    customer_id           BIGINT REFERENCES customers (id),
    notes                 TEXT,
    period_month          INTEGER,
    period_year           INTEGER,
    standard_days         INTEGER,
    extra_days            INTEGER,
    extra_hours           NUMERIC(8, 2),
    monthly_rate          NUMERIC(12, 2),
    working_days_in_month INTEGER,
    working_hours_per_day INTEGER,
    overtime_rate         NUMERIC(4, 2),
    paid_amount           NUMERIC(12, 2) DEFAULT 0,
    source_invoice_id     BIGINT,
    has_transport         BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE invoice_transports (
    id                   BIGSERIAL PRIMARY KEY,
    created_at           TIMESTAMP     NOT NULL,
    updated_at           TIMESTAMP     NOT NULL,
    deleted              BOOLEAN       NOT NULL DEFAULT false,
    deleted_at           TIMESTAMP,
    invoice_id           BIGINT        NOT NULL REFERENCES invoices (id),
    transport_date       DATE          NOT NULL,
    transport_direction  VARCHAR(255)  NOT NULL,
    transport_amount     NUMERIC(12, 2) NOT NULL
);

CREATE TABLE accounting_transactions (
    id               BIGSERIAL PRIMARY KEY,
    created_at       TIMESTAMP     NOT NULL,
    updated_at       TIMESTAMP     NOT NULL,
    deleted          BOOLEAN       NOT NULL DEFAULT false,
    deleted_at       TIMESTAMP,
    type             VARCHAR(20)   NOT NULL,
    category         VARCHAR(100)  NOT NULL,
    amount           NUMERIC(15, 2) NOT NULL,
    transaction_date DATE          NOT NULL,
    payment_method   VARCHAR(50),
    reference_number VARCHAR(100),
    description      TEXT,
    project_id       BIGINT,
    contractor_id    BIGINT,
    customer_id      BIGINT,
    notes            TEXT
);

-- ─── Debitor / Kreditor ───────────────────────────────────────────────────

CREATE TABLE receivables (
    id           BIGSERIAL PRIMARY KEY,
    created_at   TIMESTAMP     NOT NULL,
    updated_at   TIMESTAMP     NOT NULL,
    deleted      BOOLEAN       NOT NULL DEFAULT false,
    deleted_at   TIMESTAMP,
    project_id   BIGINT UNIQUE NOT NULL REFERENCES projects (id),
    customer_id  BIGINT        NOT NULL REFERENCES customers (id),
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    paid_amount  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    due_date     DATE          NOT NULL,
    status       VARCHAR(50)   NOT NULL,
    notes        TEXT
);

CREATE TABLE receivable_payments (
    id            BIGSERIAL PRIMARY KEY,
    created_at    TIMESTAMP     NOT NULL,
    updated_at    TIMESTAMP     NOT NULL,
    deleted       BOOLEAN       NOT NULL DEFAULT false,
    deleted_at    TIMESTAMP,
    receivable_id BIGINT        NOT NULL REFERENCES receivables (id),
    invoice_id    BIGINT REFERENCES invoices (id),
    amount        NUMERIC(12, 2) NOT NULL,
    payment_date  DATE          NOT NULL,
    note          VARCHAR(500)
);

CREATE TABLE payables (
    id             BIGSERIAL PRIMARY KEY,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL,
    deleted        BOOLEAN       NOT NULL DEFAULT false,
    deleted_at     TIMESTAMP,
    project_id     BIGINT UNIQUE NOT NULL REFERENCES projects (id),
    contractor_id  BIGINT REFERENCES contractors (id),
    investor_name  VARCHAR(255),
    investor_voen  VARCHAR(20),
    total_amount   NUMERIC(12, 2) NOT NULL DEFAULT 0,
    paid_amount    NUMERIC(12, 2) NOT NULL DEFAULT 0,
    due_date       DATE          NOT NULL,
    status         VARCHAR(50)   NOT NULL,
    notes          TEXT
);

CREATE TABLE payable_payments (
    id           BIGSERIAL PRIMARY KEY,
    created_at   TIMESTAMP     NOT NULL,
    updated_at   TIMESTAMP     NOT NULL,
    deleted      BOOLEAN       NOT NULL DEFAULT false,
    deleted_at   TIMESTAMP,
    payable_id   BIGINT        NOT NULL REFERENCES payables (id),
    invoice_id   BIGINT REFERENCES invoices (id),
    amount       NUMERIC(12, 2) NOT NULL,
    payment_date DATE          NOT NULL,
    note         VARCHAR(500)
);

-- ─── Dövri Xərclər ────────────────────────────────────────────────────────

CREATE TABLE recurring_expenses (
    id             BIGSERIAL PRIMARY KEY,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL,
    deleted        BOOLEAN       NOT NULL DEFAULT false,
    deleted_at     TIMESTAMP,
    name           VARCHAR(200)  NOT NULL,
    category_key   VARCHAR(100)  NOT NULL,
    category_label VARCHAR(200)  NOT NULL,
    source_key     VARCHAR(100)  NOT NULL,
    source_label   VARCHAR(200)  NOT NULL,
    amount         NUMERIC(12, 2) NOT NULL DEFAULT 0,
    frequency      VARCHAR(50)   NOT NULL,
    day_of_month   INTEGER,
    notes          TEXT,
    active         BOOLEAN       NOT NULL DEFAULT true
);

-- ─── Sənədlər (Hesab-faktura, Akt) ───────────────────────────────────────

CREATE TABLE generated_documents (
    id                    BIGSERIAL PRIMARY KEY,
    created_at            TIMESTAMP     NOT NULL,
    updated_at            TIMESTAMP     NOT NULL,
    deleted               BOOLEAN       NOT NULL DEFAULT false,
    deleted_at            TIMESTAMP,
    document_number       VARCHAR(20)   NOT NULL UNIQUE,
    document_type         VARCHAR(50)   NOT NULL,
    document_date         DATE          NOT NULL,
    customer_id           BIGINT REFERENCES customers (id),
    customer_name         VARCHAR(255)  NOT NULL,
    customer_voen         VARCHAR(20),
    customer_address      TEXT,
    customer_director_name VARCHAR(150),
    contract_date         DATE,
    contract_number       VARCHAR(100),
    subtotal              NUMERIC(12, 2) NOT NULL DEFAULT 0,
    vat_rate              NUMERIC(5, 2)  NOT NULL DEFAULT 0,
    vat_amount            NUMERIC(12, 2) NOT NULL DEFAULT 0,
    grand_total           NUMERIC(12, 2) NOT NULL DEFAULT 0,
    bank_name             VARCHAR(200),
    bank_code             VARCHAR(50),
    bank_swift            VARCHAR(20),
    bank_iban             VARCHAR(50),
    bank_mh               VARCHAR(50),
    bank_hh               VARCHAR(50),
    pdf_file_path         VARCHAR(255),
    source_invoice_ids    TEXT,
    addendum_numbers      TEXT,
    notes                 TEXT
);

CREATE TABLE document_lines (
    id               BIGSERIAL PRIMARY KEY,
    created_at       TIMESTAMP     NOT NULL,
    updated_at       TIMESTAMP     NOT NULL,
    deleted          BOOLEAN       NOT NULL DEFAULT false,
    deleted_at       TIMESTAMP,
    document_id      BIGINT        NOT NULL REFERENCES generated_documents (id),
    line_order       INTEGER       NOT NULL,
    description      TEXT          NOT NULL,
    unit             VARCHAR(50)   NOT NULL,
    quantity         NUMERIC(10, 2) NOT NULL,
    unit_price       NUMERIC(12, 2) NOT NULL,
    total_price      NUMERIC(12, 2) NOT NULL,
    source_invoice_id BIGINT
);

-- ─── Texniki Servis ───────────────────────────────────────────────────────

CREATE TABLE service_records (
    id              BIGSERIAL PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    deleted         BOOLEAN   NOT NULL DEFAULT false,
    deleted_at      TIMESTAMP,
    equipment_id    BIGINT       NOT NULL REFERENCES equipment (id),
    contractor_id   BIGINT REFERENCES contractors (id),
    service_type    VARCHAR(255) NOT NULL,
    description     TEXT,
    service_date    DATE         NOT NULL,
    next_service_date DATE,
    cost            NUMERIC(12, 2),
    odometer        INTEGER,
    notes           TEXT,
    status_before   VARCHAR(50),
    status_after    VARCHAR(50),
    completed       BOOLEAN DEFAULT false,
    record_type     VARCHAR(50),
    invoice_number  VARCHAR(255),
    invoice_date    DATE
);

CREATE TABLE service_checklist_items (
    id                BIGSERIAL PRIMARY KEY,
    service_record_id BIGINT       NOT NULL REFERENCES service_records (id),
    item_name         VARCHAR(255) NOT NULL,
    checked           BOOLEAN,
    note              VARCHAR(255)
);

-- ─── Approval / Audit ─────────────────────────────────────────────────────

CREATE TABLE pending_operations (
    id                      BIGSERIAL PRIMARY KEY,
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP NOT NULL,
    deleted                 BOOLEAN   NOT NULL DEFAULT false,
    deleted_at              TIMESTAMP,
    module_code             VARCHAR(100) NOT NULL,
    entity_type             VARCHAR(100) NOT NULL,
    entity_id               BIGINT       NOT NULL,
    entity_label            VARCHAR(255),
    operation_type          VARCHAR(50)  NOT NULL,
    performed_by_id         BIGINT       NOT NULL REFERENCES users (id),
    performer_department_id BIGINT REFERENCES departments (id),
    old_snapshot            TEXT,
    new_snapshot            TEXT,
    status                  VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    processed_by_id         BIGINT REFERENCES users (id),
    processed_at            TIMESTAMP,
    reject_reason           TEXT
);

CREATE TABLE audit_logs (
    id           BIGSERIAL PRIMARY KEY,
    entity_type  VARCHAR(255) NOT NULL,
    entity_id    BIGINT,
    entity_label VARCHAR(255),
    action       VARCHAR(255) NOT NULL,
    performed_by VARCHAR(255),
    performed_at TIMESTAMP    NOT NULL,
    summary      VARCHAR(255)
);
