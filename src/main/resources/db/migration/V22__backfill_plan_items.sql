-- ════════════════════════════════════════════════════════════════════════════
-- V22 — Köhnə (tək-texnika) koordinator planlarını çoxlu xətt modelinə miqrasiya
-- Hər mövcud planın legacy sahələrindən (selectedEquipment + qiymət + icra + operator)
-- bir CoordinatorPlanItem yaradılır. Beləliklə bütün köhnə layihələr də xətt modelində
-- görünür və legacy körpüyə ehtiyac qalmır.
-- Yalnız texnikası olan və hələ xətti olmayan planlar üçün işləyir (təkrarsız).
-- ════════════════════════════════════════════════════════════════════════════

INSERT INTO coordinator_plan_items (
    created_at, updated_at, deleted,
    plan_id, shortlist_item_id, party_type, contractor_id, investor_id, equipment_id,
    equipment_price, customer_equipment_price, transportation_price,
    day_count, start_date, end_date, operator_id,
    equipment_docs_verified, equipment_docs_checked_at, dispatched_at, delivered_at, delivery_notes
)
SELECT
    now(), now(), false,
    cp.id,
    cp.winner_item_id,
    COALESCE(si.party_type,
        CASE e.ownership_type
            WHEN 'CONTRACTOR' THEN 'CONTRACTOR'
            WHEN 'INVESTOR'   THEN 'INVESTOR'
            ELSE 'COMPANY'
        END),
    si.contractor_id,
    si.investor_id,
    COALESCE(cp.equipment_id, si.equipment_id),
    cp.equipment_price,
    cp.customer_equipment_price,
    cp.transportation_price,
    cp.day_count,
    cp.start_date,
    cp.end_date,
    cp.operator_id,
    COALESCE(cp.equipment_docs_verified, false),
    cp.equipment_docs_checked_at,
    cp.dispatched_at,
    cp.delivered_at,
    cp.delivery_notes
FROM coordinator_plans cp
LEFT JOIN shortlist_items si ON si.id = cp.winner_item_id
LEFT JOIN equipment e ON e.id = COALESCE(cp.equipment_id, si.equipment_id)
WHERE cp.deleted = false
  AND COALESCE(cp.equipment_id, si.equipment_id) IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM coordinator_plan_items cpi
      WHERE cpi.plan_id = cp.id AND cpi.deleted = false
  );
