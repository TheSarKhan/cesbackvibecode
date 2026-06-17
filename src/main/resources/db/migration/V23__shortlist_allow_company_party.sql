-- ════════════════════════════════════════════════════════════════════════════
-- V23 — Shortlist sətrində ŞİRKƏT (COMPANY) mənbəyinə icazə
-- Köhnə CHECK constraint-lər (V6: chk_shortlist_party, V7: shortlist_items_party_type_check)
-- yalnız CONTRACTOR/INVESTOR-a icazə verirdi. Çoxlu-texnika flow-da artıq şirkətin
-- öz texnikası da mənbə kimi seçilə bilir (party_type='COMPANY', contractor/investor NULL).
-- Hər iki köhnə constraint-i atırıq və hər üç tipi düzgün münasibətlə əhatə edən
-- tək constraint əlavə edirik.
-- ════════════════════════════════════════════════════════════════════════════

ALTER TABLE shortlist_items DROP CONSTRAINT IF EXISTS chk_shortlist_party;
ALTER TABLE shortlist_items DROP CONSTRAINT IF EXISTS shortlist_items_party_type_check;

ALTER TABLE shortlist_items
    ADD CONSTRAINT chk_shortlist_party CHECK (
        (party_type = 'COMPANY'    AND contractor_id IS NULL     AND investor_id IS NULL)
        OR (party_type = 'CONTRACTOR' AND contractor_id IS NOT NULL AND investor_id IS NULL)
        OR (party_type = 'INVESTOR'   AND investor_id IS NOT NULL  AND contractor_id IS NULL)
    );
