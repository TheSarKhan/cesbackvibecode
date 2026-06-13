-- ════════════════════════════════════════════════════════════════════════════
-- V11 — Dinamik icazə kataloqu + Role↔Permission & User↔Roles çoxa-çox + Super Admin flag
-- Mövcud boolean role_permissions grant-ları birə-bir köçürülür (heç bir icazə itmir/qazanılmır).
-- ════════════════════════════════════════════════════════════════════════════

-- 1) Dinamik icazə kataloqu
CREATE TABLE permission (
    id              BIGSERIAL PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    deleted         BOOLEAN   NOT NULL DEFAULT false,
    deleted_at      TIMESTAMP,
    code            VARCHAR(255) NOT NULL UNIQUE,
    module_code     VARCHAR(255) NOT NULL,
    action          VARCHAR(255) NOT NULL,
    label_az        VARCHAR(255) NOT NULL,
    description     VARCHAR(255),
    auto_discovered BOOLEAN   NOT NULL DEFAULT false
);

-- 2) Role ↔ Permission (çoxa-çox)
CREATE TABLE role_granted_permission (
    role_id       BIGINT NOT NULL REFERENCES roles (id),
    permission_id BIGINT NOT NULL REFERENCES permission (id),
    CONSTRAINT uq_role_granted_permission UNIQUE (role_id, permission_id)
);

-- 3) User ↔ Roles (çoxa-çox)
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id),
    role_id BIGINT NOT NULL REFERENCES roles (id),
    CONSTRAINT uq_user_roles UNIQUE (user_id, role_id)
);

-- 4) Super Admin flag
ALTER TABLE roles ADD COLUMN is_super_admin BOOLEAN NOT NULL DEFAULT false;
UPDATE roles SET is_super_admin = true WHERE name = 'Super Admin';

-- 5) Mövcud boolean grant-ları köçür → permission kataloqu
INSERT INTO permission (created_at, updated_at, deleted, code, module_code, action, label_az, auto_discovered)
SELECT now(), now(), false, code, module_code, action, label_az, false FROM (
    SELECT DISTINCT sm.code || ':GET'              AS code, sm.code AS module_code, 'GET'              AS action, sm.name_az || ' — Oxumaq'                  AS label_az FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id WHERE rp.can_get
    UNION SELECT DISTINCT sm.code || ':POST',             sm.code, 'POST',             sm.name_az || ' — Yazmaq'                  FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id WHERE rp.can_post
    UNION SELECT DISTINCT sm.code || ':PUT',              sm.code, 'PUT',              sm.name_az || ' — Redaktə'                 FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id WHERE rp.can_put
    UNION SELECT DISTINCT sm.code || ':DELETE',           sm.code, 'DELETE',           sm.name_az || ' — Silmək'                  FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id WHERE rp.can_delete
    UNION SELECT DISTINCT sm.code || ':SEND_COORDINATOR', sm.code, 'SEND_COORDINATOR', sm.name_az || ' — Koordinatora göndər'     FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id WHERE rp.can_send_to_coordinator
    UNION SELECT DISTINCT sm.code || ':SUBMIT_OFFER',     sm.code, 'SUBMIT_OFFER',     sm.name_az || ' — Təklif göndər'           FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id WHERE rp.can_submit_offer
    UNION SELECT DISTINCT sm.code || ':SEND_ACCOUNTING',  sm.code, 'SEND_ACCOUNTING',  sm.name_az || ' — Mühasibatlığa göndər'    FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id WHERE rp.can_send_to_accounting
    UNION SELECT DISTINCT sm.code || ':RETURN_PROJECT',   sm.code, 'RETURN_PROJECT',   sm.name_az || ' — Layihəyə geri göndər'    FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id WHERE rp.can_return_to_project
    UNION SELECT DISTINCT sm.code || ':APPROVE_PM',       sm.code, 'APPROVE_PM',       sm.name_az || ' — PM təsdiqi'              FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id WHERE rp.can_approve_by_pm
    UNION SELECT DISTINCT sm.code || ':CHECK_DOCUMENTS',  sm.code, 'CHECK_DOCUMENTS',  sm.name_az || ' — Sənəd təsdiqi'           FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id WHERE rp.can_check_documents
    UNION SELECT DISTINCT sm.code || ':DISPATCH',         sm.code, 'DISPATCH',         sm.name_az || ' — Texnika göndər'          FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id WHERE rp.can_dispatch
    UNION SELECT DISTINCT sm.code || ':DELIVER',          sm.code, 'DELIVER',          sm.name_az || ' — Təhvil-təslim'           FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id WHERE rp.can_deliver
) src
ON CONFLICT (code) DO NOTHING;

-- 6) Mövcud grant-ları role_granted_permission-a linklə (hər boolean sütun → uyğun code)
INSERT INTO role_granted_permission (role_id, permission_id)
SELECT rp.role_id, p.id FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id JOIN permission p ON p.code = sm.code || ':GET'              WHERE rp.can_get
UNION SELECT rp.role_id, p.id FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id JOIN permission p ON p.code = sm.code || ':POST'             WHERE rp.can_post
UNION SELECT rp.role_id, p.id FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id JOIN permission p ON p.code = sm.code || ':PUT'              WHERE rp.can_put
UNION SELECT rp.role_id, p.id FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id JOIN permission p ON p.code = sm.code || ':DELETE'           WHERE rp.can_delete
UNION SELECT rp.role_id, p.id FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id JOIN permission p ON p.code = sm.code || ':SEND_COORDINATOR' WHERE rp.can_send_to_coordinator
UNION SELECT rp.role_id, p.id FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id JOIN permission p ON p.code = sm.code || ':SUBMIT_OFFER'     WHERE rp.can_submit_offer
UNION SELECT rp.role_id, p.id FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id JOIN permission p ON p.code = sm.code || ':SEND_ACCOUNTING'  WHERE rp.can_send_to_accounting
UNION SELECT rp.role_id, p.id FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id JOIN permission p ON p.code = sm.code || ':RETURN_PROJECT'   WHERE rp.can_return_to_project
UNION SELECT rp.role_id, p.id FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id JOIN permission p ON p.code = sm.code || ':APPROVE_PM'       WHERE rp.can_approve_by_pm
UNION SELECT rp.role_id, p.id FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id JOIN permission p ON p.code = sm.code || ':CHECK_DOCUMENTS'  WHERE rp.can_check_documents
UNION SELECT rp.role_id, p.id FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id JOIN permission p ON p.code = sm.code || ':DISPATCH'         WHERE rp.can_dispatch
UNION SELECT rp.role_id, p.id FROM role_permissions rp JOIN system_modules sm ON rp.module_id = sm.id JOIN permission p ON p.code = sm.code || ':DELIVER'          WHERE rp.can_deliver
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 7) Mövcud tək rol → user_roles (geriyə uyğunluq)
INSERT INTO user_roles (user_id, role_id)
SELECT id, role_id FROM users WHERE role_id IS NOT NULL
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 8) Köhnə strukturu sil
ALTER TABLE users DROP COLUMN role_id;
DROP TABLE role_permissions;
