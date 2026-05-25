-- V8: Flow optimallaşdırma — PM/Koordinator addımları üçün əlavə sahələr
-- LM 1.3: sifarişçi ofis kontaktı | LM 3.1: ayrılmış razılaşma qiymətləri
-- Koordinator: shortlist rank + qalib sətir referansı

-- ─── ShortlistItem: priority rank ─────────────────────────────────────────
ALTER TABLE shortlist_items
    ADD COLUMN IF NOT EXISTS priority_rank INTEGER;

CREATE INDEX IF NOT EXISTS idx_shortlist_item_rank
    ON shortlist_items (priority_rank);

-- ─── TechRequest: customer office contact + ayrılmış razılaşma qiymətləri ─
ALTER TABLE tech_requests
    ADD COLUMN IF NOT EXISTS customer_office_contact VARCHAR(255),
    ADD COLUMN IF NOT EXISTS customer_office_phone   VARCHAR(50),
    ADD COLUMN IF NOT EXISTS agreed_equipment_price  NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS agreed_transport_price  NUMERIC(12, 2);

-- ─── CoordinatorPlan: qalib shortlist sətir referansı ─────────────────────
ALTER TABLE coordinator_plans
    ADD COLUMN IF NOT EXISTS winner_item_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'coordinator_plans'
          AND constraint_name = 'fk_coordinator_plans_winner_item'
    ) THEN
        ALTER TABLE coordinator_plans
            ADD CONSTRAINT fk_coordinator_plans_winner_item
            FOREIGN KEY (winner_item_id) REFERENCES shortlist_items (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_coordinator_plans_winner_item
    ON coordinator_plans (winner_item_id);
