-- ════════════════════════════════════════════════════════════════════════════
-- V24 — Hər texnika xətti üçün ayrıca sifarişçi razılaşması + ayrıca sənədlər
-- PM "Razılaşma" mərhələsində hər texnika xətti öz razılaşdırılmış qiymətini,
-- daşınmasını, qeydini daşıyır. Müqavilə və qiymət protokolu sənədləri də artıq
-- konkret xəttə bağlana bilir (request_documents.plan_item_id).
-- ════════════════════════════════════════════════════════════════════════════

-- Xətt üzrə razılaşma sahələri
ALTER TABLE coordinator_plan_items ADD COLUMN agreed_equipment_price NUMERIC(12, 2);
ALTER TABLE coordinator_plan_items ADD COLUMN agreed_transport_price NUMERIC(12, 2);
ALTER TABLE coordinator_plan_items ADD COLUMN agreed_total_price     NUMERIC(12, 2);
ALTER TABLE coordinator_plan_items ADD COLUMN agreement_note         TEXT;

-- Sənədi (Müqavilə / Qiymət protokolu) konkret texnika xəttinə bağlamaq üçün
ALTER TABLE request_documents
    ADD COLUMN plan_item_id BIGINT REFERENCES coordinator_plan_items (id);

CREATE INDEX IF NOT EXISTS idx_reqdoc_plan_item ON request_documents (plan_item_id);
