-- ════════════════════════════════════════════════════════════════════════════
-- V14 — İstifadəçilər (EMPLOYEE_MANAGEMENT) öz modulu + Dashboard grantable (DASHBOARD:GET)
-- Məqsəd: mövcud giriş itməsin.
--   • İstifadəçi idarəsi ROLE_PERMISSION-dan ayrılır → EMPLOYEE_MANAGEMENT (modul artıq mövcuddur).
--     ROLE_PERMISSION rol/icazə idarəsi üçün qalır.
--   • Dashboard hər rola verilir (əvvəl hamıya açıq idi).
-- Qeyd: auto_discovered=false → phantom-prune toxunmur; endpoint-lər də scan ilə kodları saxlayır.
-- ════════════════════════════════════════════════════════════════════════════

-- 1) EMPLOYEE_MANAGEMENT icazə sətirləri
INSERT INTO permission (created_at, updated_at, deleted, code, module_code, action, label_az, auto_discovered)
SELECT now(), now(), false, 'EMPLOYEE_MANAGEMENT:' || a.suffix, 'EMPLOYEE_MANAGEMENT', a.suffix,
       'İstifadəçi İdarəetməsi — ' || a.label_az, false
FROM (VALUES
    ('GET', 'Oxumaq'),
    ('POST', 'Yazmaq'),
    ('PUT', 'Redaktə'),
    ('DELETE', 'Silmək')
) AS a(suffix, label_az)
ON CONFLICT (code) DO NOTHING;

-- ROLE_PERMISSION:X qrantı olan rollara müvafiq EMPLOYEE_MANAGEMENT:X ver
-- (istifadəçi idarəsi olan rollar davam etsin)
INSERT INTO role_granted_permission (role_id, permission_id)
SELECT DISTINCT rgp.role_id, pnew.id
FROM role_granted_permission rgp
JOIN permission pold ON pold.id = rgp.permission_id AND pold.module_code = 'ROLE_PERMISSION'
JOIN permission pnew ON pnew.code = 'EMPLOYEE_MANAGEMENT:' || pold.action
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 2) DASHBOARD:GET icazə sətri
INSERT INTO permission (created_at, updated_at, deleted, code, module_code, action, label_az, auto_discovered)
VALUES (now(), now(), false, 'DASHBOARD:GET', 'DASHBOARD', 'GET', 'İdarə paneli — Oxumaq', false)
ON CONFLICT (code) DO NOTHING;

-- Mövcud bütün rollara DASHBOARD:GET ver (heç bir rol dashboard girişini itirməsin)
INSERT INTO role_granted_permission (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permission p
WHERE p.code = 'DASHBOARD:GET'
ON CONFLICT (role_id, permission_id) DO NOTHING;
