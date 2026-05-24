-- ============================================================================
-- V6: Yeni Sorğu → PM → Koordinator → Mühasibat → Koordinator Flow
-- ============================================================================
--
-- Bu migrasiya köhnə request flowunu yeni 5-mərhələli flowla əvəz edir.
-- İstifadəçi razılıq verib mövcud sorğu/koordinator/layihə datasının silinməsi
-- üçün (prod-da deyil). Yeni cədvəllər və icazə sütunları yaradılır.
-- ============================================================================

-- ─── 1. Köhnə datanın silinməsi (cascade ardıcıllığı) ──────────────────────
-- Project-ə bağlı invoice/receivable/payable hierarchy əvvəl təmizlənməlidir.

-- Invoice hierarchy
DELETE FROM invoice_transports;
DELETE FROM document_lines;
DELETE FROM generated_documents;
DELETE FROM receivable_payments;
DELETE FROM payable_payments;
DELETE FROM receivables;
DELETE FROM payables;
DELETE FROM invoices;

-- Project hierarchy
DELETE FROM project_payment_entries;
DELETE FROM project_expenses;
DELETE FROM project_revenues;
DELETE FROM projects;

-- Coordinator plan hierarchy
DELETE FROM coordinator_plan_safety_items;
DELETE FROM coordinator_documents;
DELETE FROM coordinator_plans;

-- Equipment-project tarixçəsi (project_id FK deyil amma orphan qalmasın)
DELETE FROM equipment_project_history;

-- Request hierarchy
DELETE FROM request_status_logs;
DELETE FROM tech_request_params;
DELETE FROM tech_requests;

-- ─── 1b. TechRequest-ə agreedTotalPrice sütunu ──────────────────────────

ALTER TABLE tech_requests
    ADD COLUMN IF NOT EXISTS agreed_total_price NUMERIC(12,2);

-- ─── 2. CoordinatorPlan-a yeni sütunlar ────────────────────────────────────

ALTER TABLE coordinator_plans
    ADD COLUMN IF NOT EXISTS transport_contractor_id BIGINT;

-- PostgreSQL-də `ADD CONSTRAINT IF NOT EXISTS` yoxdur — DO blok ilə yoxlayırıq
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_coord_plan_transport_contractor'
    ) THEN
        ALTER TABLE coordinator_plans
            ADD CONSTRAINT fk_coord_plan_transport_contractor
            FOREIGN KEY (transport_contractor_id) REFERENCES contractors(id);
    END IF;
END $$;

ALTER TABLE coordinator_plans
    ADD COLUMN IF NOT EXISTS equipment_docs_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS equipment_docs_checked_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS dispatched_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS delivery_notes TEXT;

-- ─── 3. RolePermission-a yeni custom action sütunları ─────────────────────

ALTER TABLE role_permissions
    ADD COLUMN IF NOT EXISTS can_approve_by_pm BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS can_check_documents BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS can_dispatch BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS can_deliver BOOLEAN NOT NULL DEFAULT FALSE;

-- ─── 4. PROJECT_MANAGER modulu (DataSeeder boş DB-də işlədikdə qaçırılacaq;
--     mövcud DB-də manual əlavə edirik) ─────────────────────────────────

INSERT INTO system_modules (code, name_az, name_en, order_index, created_at, updated_at, deleted)
SELECT 'PROJECT_MANAGER', 'Layihə Meneceri', 'Layihə Meneceri', 7, NOW(), NOW(), FALSE
WHERE NOT EXISTS (SELECT 1 FROM system_modules WHERE code = 'PROJECT_MANAGER');

-- Super Admin (varsa) rolu üçün PROJECT_MANAGER modulu üzərində bütün hüquqlar
INSERT INTO role_permissions (role_id, module_id, can_get, can_post, can_put, can_delete,
                               can_send_to_coordinator, can_submit_offer, can_send_to_accounting,
                               can_return_to_project, can_approve_by_pm, can_check_documents,
                               can_dispatch, can_deliver)
SELECT r.id, m.id, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM roles r, system_modules m
WHERE r.name = 'Super Admin'
  AND m.code = 'PROJECT_MANAGER'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.module_id = m.id
  );

-- Super Admin üçün mövcud icazələri də yeni flag-lərlə yenilə
UPDATE role_permissions
SET can_approve_by_pm = TRUE,
    can_check_documents = TRUE,
    can_dispatch = TRUE,
    can_deliver = TRUE
WHERE role_id IN (SELECT id FROM roles WHERE name = 'Super Admin');

-- ─── 5. Yeni cədvəllər ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS request_shortlists (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL UNIQUE REFERENCES tech_requests(id),
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_shortlist_request ON request_shortlists(request_id);
CREATE INDEX IF NOT EXISTS idx_shortlist_deleted ON request_shortlists(deleted);

CREATE TABLE IF NOT EXISTS shortlist_items (
    id BIGSERIAL PRIMARY KEY,
    shortlist_id BIGINT NOT NULL REFERENCES request_shortlists(id),
    party_type VARCHAR(20) NOT NULL,
    contractor_id BIGINT REFERENCES contractors(id),
    investor_id BIGINT REFERENCES investors(id),
    equipment_id BIGINT REFERENCES equipment(id),
    negotiated_price NUMERIC(12,2),
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_shortlist_party CHECK (
        (party_type = 'CONTRACTOR' AND contractor_id IS NOT NULL AND investor_id IS NULL)
        OR (party_type = 'INVESTOR' AND investor_id IS NOT NULL AND contractor_id IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_shortlist_item_list ON shortlist_items(shortlist_id);
CREATE INDEX IF NOT EXISTS idx_shortlist_item_deleted ON shortlist_items(deleted);

CREATE TABLE IF NOT EXISTS request_documents (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL REFERENCES tech_requests(id),
    doc_type VARCHAR(30) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    uploaded_by_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reqdoc_request ON request_documents(request_id);
CREATE INDEX IF NOT EXISTS idx_reqdoc_type ON request_documents(doc_type);
CREATE INDEX IF NOT EXISTS idx_reqdoc_deleted ON request_documents(deleted);
