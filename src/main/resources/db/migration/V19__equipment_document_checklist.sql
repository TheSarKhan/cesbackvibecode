-- ════════════════════════════════════════════════════════════════════════════
-- V19 — Texnika sənəd checklist mexanizmi
-- 1. Texnikanın məcburi sənəd tipləri (equipment ↔ config_item ManyToMany)
-- 2. LM-in sorğuya əlavə etdiyi tələb olunan sənədlər (tech_request ↔ config_item)
-- 3. Koordinatorun yoxlama zamanı işarələdiyi sənədlər (coordinator_plan element coll.)
-- 4. EQUIPMENT_DOCUMENT_TYPE config siyahısının ilkin seed-i (enum YOX — cədvəl)
-- ════════════════════════════════════════════════════════════════════════════

-- 1) Texnikanın məcburi sənədləri (təhlükəsizlik avadanlığı pattern-i kimi)
CREATE TABLE equipment_required_documents (
    equipment_id   BIGINT NOT NULL REFERENCES equipment (id),
    config_item_id BIGINT NOT NULL REFERENCES config_items (id),
    PRIMARY KEY (equipment_id, config_item_id)
);

-- 2) LM-in sorğu üçün tələb etdiyi əlavə sənədlər
CREATE TABLE request_required_documents (
    request_id     BIGINT NOT NULL REFERENCES tech_requests (id),
    config_item_id BIGINT NOT NULL REFERENCES config_items (id),
    PRIMARY KEY (request_id, config_item_id)
);

-- 3) Koordinatorun yoxlama zamanı işarələdiyi sənəd tipləri
CREATE TABLE coordinator_plan_checked_docs (
    plan_id        BIGINT NOT NULL REFERENCES coordinator_plans (id),
    config_item_id BIGINT NOT NULL REFERENCES config_items (id),
    PRIMARY KEY (plan_id, config_item_id)
);

-- 4) Sənəd tiplərinin ilkin siyahısı (admin "Konfiqurasiya"dan əlavə edə bilər)
INSERT INTO config_items (created_at, updated_at, deleted, category, item_key, sort_order, active)
VALUES
    (now(), now(), false, 'EQUIPMENT_DOCUMENT_TYPE', 'Qeydiyyat şəhadətnaməsi', 1, true),
    (now(), now(), false, 'EQUIPMENT_DOCUMENT_TYPE', 'Texniki pasport',          2, true),
    (now(), now(), false, 'EQUIPMENT_DOCUMENT_TYPE', 'Texniki müayinə aktı',     3, true),
    (now(), now(), false, 'EQUIPMENT_DOCUMENT_TYPE', 'Üçüncü tərəf müayinəsi',    4, true),
    (now(), now(), false, 'EQUIPMENT_DOCUMENT_TYPE', 'Sığorta polisi',           5, true),
    (now(), now(), false, 'EQUIPMENT_DOCUMENT_TYPE', 'İcazə / Lisenziya',        6, true)
ON CONFLICT (category, item_key) DO NOTHING;
