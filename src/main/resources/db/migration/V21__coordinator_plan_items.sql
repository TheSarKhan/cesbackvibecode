-- ════════════════════════════════════════════════════════════════════════════
-- V21 — Çoxlu texnika: koordinator plan xətləri (Faza 1, additive)
-- 1 layihədə birdən çox texnika seçilə bilir. Hər seçilmiş texnika bir xətt olur
-- və öz qiymətini, müddətini, operatorunu, sənəd yoxlamasını, göndərmə/təhvil
-- vəziyyətini daşıyır. Bu addım yalnız yeni cədvəllər əlavə edir — mövcud
-- tək-texnika sahələri toxunulmaz qalır (sonrakı fazalarda miqrasiya olunur).
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE coordinator_plan_items (
    id                        BIGSERIAL PRIMARY KEY,
    created_at                TIMESTAMP NOT NULL,
    updated_at                TIMESTAMP NOT NULL,
    deleted                   BOOLEAN   NOT NULL DEFAULT false,
    deleted_at                TIMESTAMP,
    plan_id                   BIGINT    NOT NULL REFERENCES coordinator_plans (id),
    shortlist_item_id         BIGINT    REFERENCES shortlist_items (id),
    party_type                VARCHAR(20),
    contractor_id             BIGINT    REFERENCES contractors (id),
    investor_id               BIGINT    REFERENCES investors (id),
    equipment_id              BIGINT    REFERENCES equipment (id),
    equipment_price           NUMERIC(12, 2),
    customer_equipment_price  NUMERIC(12, 2),
    transportation_price      NUMERIC(12, 2),
    day_count                 INTEGER,
    start_date                DATE,
    end_date                  DATE,
    operator_id               BIGINT    REFERENCES operators (id),
    equipment_docs_verified   BOOLEAN   NOT NULL DEFAULT false,
    equipment_docs_checked_at TIMESTAMP,
    dispatched_at             TIMESTAMP,
    delivered_at              TIMESTAMP,
    delivery_notes            TEXT
);

CREATE INDEX idx_cpi_plan    ON coordinator_plan_items (plan_id);
CREATE INDEX idx_cpi_deleted ON coordinator_plan_items (deleted);

-- Hər xəttin yoxlama checklist-ində işarələnmiş sənəd tipləri
CREATE TABLE coordinator_plan_item_checked_docs (
    plan_item_id   BIGINT NOT NULL REFERENCES coordinator_plan_items (id),
    config_item_id BIGINT NOT NULL REFERENCES config_items (id),
    PRIMARY KEY (plan_item_id, config_item_id)
);

-- Sənəd (məs. təhvil-təslim aktı) konkret texnika xəttinə bağlana bilsin
ALTER TABLE coordinator_documents ADD COLUMN plan_item_id BIGINT REFERENCES coordinator_plan_items (id);
