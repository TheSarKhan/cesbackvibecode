-- ════════════════════════════════════════════════════════════════════════════
-- V25 — Kreditor (Payable) sahib başına: 1 layihədə bir neçə payable
-- Çoxlu texnika modelində 1 layihənin müxtəlif sahibləri (podratçı/investor) ola
-- bilər. Əvvəl payable layihə başına TƏK idi (project_id UNIQUE) və hər yeni xərc
-- qaiməsi məbləği üzərinə yazırdı → digər sahiblərin borcu itirdi. İndi hər (layihə
-- + sahib) üçün ayrı payable saxlanılır, ona görə project_id unikallığı qaldırılır.
-- ════════════════════════════════════════════════════════════════════════════

ALTER TABLE payables DROP CONSTRAINT IF EXISTS payables_project_id_key;

CREATE INDEX IF NOT EXISTS idx_payables_project ON payables (project_id);
