-- ════════════════════════════════════════════════════════════════════════════
-- V12 — Super Admin "xüsusi flag"-dan tam permission-əsaslı modelə keçid.
-- is_super_admin rolları bütün mövcud icazələri REAL qrant kimi alır, sonra flag silinir.
-- Beləcə super admin adi permission-əsaslı rola çevrilir (xüsusi məntiq yox).
-- (Xəyali icazələr varsa, startup-da PermissionScanner həm onları, həm bu qrant linklərini təmizləyir.)
-- ════════════════════════════════════════════════════════════════════════════

-- Super admin rol(lar)ına bütün icazələri ver (flag silinməzdən əvvəl)
INSERT INTO role_granted_permission (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permission p
WHERE r.is_super_admin = true
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Flag artıq lazım deyil — hər şey qrant-əsaslıdır
ALTER TABLE roles DROP COLUMN is_super_admin;
