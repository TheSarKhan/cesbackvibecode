-- ════════════════════════════════════════════════════════════════════════════
-- V27 — Toplu qaimə: qaimə sətirləri (bir qaimədə çoxlu texnika)
-- Əvvəl 1 qaimə = 1 texnika idi. İndi eyni layihənin texnikaları bir qaimənin
-- içində sətir-sətir ola bilər; qaimə məbləği sətirlərin cəmidir. Köhnə qaimələr
-- sətirsiz qalır (Invoice.equipment/equipmentName geriyə uyğunluq).
-- Daşınma qaimə səviyyəsində qalır; hər transport equipment_id ilə işarələnir.
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE invoice_lines (
    id                    BIGSERIAL PRIMARY KEY,
    created_at            TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP NOT NULL,
    deleted               BOOLEAN   NOT NULL DEFAULT false,
    deleted_at            TIMESTAMP,
    invoice_id            BIGINT       NOT NULL REFERENCES invoices (id),
    equipment_id          BIGINT       REFERENCES equipment (id),
    equipment_name        VARCHAR(255),
    plan_item_id          BIGINT,
    unit_price            NUMERIC(12,2),
    day_count             INTEGER,
    period_month          INTEGER,
    period_year           INTEGER,
    standard_days         INTEGER,
    extra_days            INTEGER,
    extra_hours           NUMERIC(8,2),
    monthly_rate          NUMERIC(12,2),
    working_days_in_month INTEGER,
    working_hours_per_day INTEGER,
    overtime_rate         NUMERIC(4,2),
    equipment_amount      NUMERIC(12,2),
    transport_amount      NUMERIC(12,2) DEFAULT 0,
    line_total            NUMERIC(12,2)
);

CREATE INDEX idx_invoice_lines_invoice ON invoice_lines (invoice_id);

-- Daşınmanı texnikaya bağlamaq üçün (toplu qaimədə sətir qruplaşması)
ALTER TABLE invoice_transports ADD COLUMN IF NOT EXISTS equipment_id BIGINT;
