-- ════════════════════════════════════════════════════════════════════════════
-- V20 — Sorğuya ödəniş növü (nağd / köçürmə)
-- Sorğu yaradılarkən layihənin nağd (CASH) yoxsa köçürmə (TRANSFER) olduğu qeyd edilir.
-- ════════════════════════════════════════════════════════════════════════════

ALTER TABLE tech_requests ADD COLUMN payment_method VARCHAR(20);
