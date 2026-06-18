-- ════════════════════════════════════════════════════════════════════════════
-- V28 — Hər texnikanın öz təhvil-təslim aktı (sətir başına akt)
-- Toplu qaimədə hər texnika sətrinin ayrı təhvil-təslim aktı olur.
-- ════════════════════════════════════════════════════════════════════════════

ALTER TABLE invoice_lines ADD COLUMN IF NOT EXISTS akt_file_path VARCHAR(500);
ALTER TABLE invoice_lines ADD COLUMN IF NOT EXISTS akt_file_name VARCHAR(255);
