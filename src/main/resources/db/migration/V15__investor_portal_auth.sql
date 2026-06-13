-- ════════════════════════════════════════════════════════════════════════════
-- V15 — Investor Portal kimlik doğrulaması üçün sütunlar
-- Investor şirkət-içi User DEYİL; portal girişi ayrı (accountEmail + passwordHash).
-- Mövcud investorlar portalEnabled=false (giriş bağlı) qalır.
-- passwordHash heç bir API cavabında qaytarılmır.
-- ════════════════════════════════════════════════════════════════════════════

ALTER TABLE investors ADD COLUMN account_email  VARCHAR(255);
ALTER TABLE investors ADD COLUMN password_hash  VARCHAR(255);
ALTER TABLE investors ADD COLUMN portal_enabled BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE investors ADD COLUMN last_login_at  TIMESTAMP;

-- account_email unikal (Postgres çoxlu NULL-a icazə verir, yalnız təyin olunmuşlar unikal)
ALTER TABLE investors ADD CONSTRAINT uq_investors_account_email UNIQUE (account_email);
