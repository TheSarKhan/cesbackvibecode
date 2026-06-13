-- Təsdiq diff-i üçün oxunaqlı "sonrakı" snapshot sütunu.
-- applyEdit hələ də new_snapshot (request DTO) işlədir; bu sütun yalnız göstərim üçündür.
ALTER TABLE pending_operations ADD COLUMN new_snapshot_view TEXT;
