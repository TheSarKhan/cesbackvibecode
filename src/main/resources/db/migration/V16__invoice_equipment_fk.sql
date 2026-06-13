-- ════════════════════════════════════════════════════════════════════════════
-- V16 — Qaiməyə texnika FK (equipment_id)
-- Məqsəd: investor qazanc hesabatı üçün qaimə↔texnika bağlantısını AD ilə deyil,
-- ID ilə möhkəm etmək. Mövcud data layihə → tələb → seçilmiş texnika ID zənciri
-- ilə doldurulur (adla uyğunlaşdırma YOX — dəqiq və səhvsiz).
-- Sütun nullable: layihəsiz/texnikasız qaimələr (məs. bəzi şirkət-içi xərclər) NULL qalır.
-- ════════════════════════════════════════════════════════════════════════════

ALTER TABLE invoices ADD COLUMN equipment_id BIGINT;

ALTER TABLE invoices ADD CONSTRAINT fk_invoices_equipment
    FOREIGN KEY (equipment_id) REFERENCES equipment (id);

-- Backfill: invoices.project_id → projects.request_id → tech_requests.equipment_id
UPDATE invoices i
SET equipment_id = tr.equipment_id
FROM projects p
JOIN tech_requests tr ON tr.id = p.request_id
WHERE i.project_id = p.id
  AND i.project_id IS NOT NULL
  AND tr.equipment_id IS NOT NULL;

CREATE INDEX idx_invoices_equipment ON invoices (equipment_id);

-- ────────────────────────────────────────────────────────────────────────────
-- İnvestor FK backfill: avtomatik INVESTOR_EXPENSE qaimələrində investor_id heç
-- vaxt set edilməyib (yalnız company_name). Portal dashboard/invoices investor_id
-- ilə süzür. Yuxarıda doldurulmuş equipment_id → equipment.owner_investor_voen →
-- investors.voen zənciri ilə bağlayırıq (VÖEN unikaldır — adla yox, dəqiq).
-- ────────────────────────────────────────────────────────────────────────────
UPDATE invoices i
SET investor_id = inv.id
FROM equipment e
JOIN investors inv ON inv.voen = e.owner_investor_voen AND inv.deleted = false
WHERE i.equipment_id = e.id
  AND i.type = 'INVESTOR_EXPENSE'
  AND i.investor_id IS NULL
  AND e.owner_investor_voen IS NOT NULL;
