-- ════════════════════════════════════════════════════════════════════════════
-- V18 — Mövcud investor Payable-larında investor_voen backfill
-- Problem: syncPayableDebt() INVESTOR_EXPENSE Payable yaradanda investor_voen=NULL
-- qoyurdu. İnvestor app /portal/payments endpoint-i payables-ı investor_voen ilə
-- süzür (findAllByInvestorVoen), ona görə bu köhnə ödənişlər app-da heç görünmürdü.
-- Kod artıq düzəlib (yeni Payable-lar + addPayment self-heal), bu migration isə
-- BAZADAKI MÖVCUD yazıları doldurur ki, köhnə ödənişlər də dərhal görünsün.
--
-- Bağlantı: payables.project_id → invoices(INVESTOR_EXPENSE).investor_id → investors.voen
-- (invoices.investor_id V16-da artıq backfill olunub — VÖEN unikaldır, adla yox, dəqiq.)
-- Yalnız investor Payable-ları (contractor_id IS NULL) və boş investor_voen yenilənir.
-- ════════════════════════════════════════════════════════════════════════════

UPDATE payables p
SET investor_voen = sub.voen
FROM (
    SELECT i.project_id, MIN(inv.voen) AS voen
    FROM invoices i
    JOIN investors inv ON inv.id = i.investor_id AND inv.deleted = false
    WHERE i.type = 'INVESTOR_EXPENSE'
      AND i.investor_id IS NOT NULL
      AND i.deleted = false
    GROUP BY i.project_id
) sub
WHERE p.project_id = sub.project_id
  AND p.contractor_id IS NULL
  AND (p.investor_voen IS NULL OR p.investor_voen = '')
  AND p.deleted = false;
