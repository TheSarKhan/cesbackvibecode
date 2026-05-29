-- ─────────────────────────────────────────────────────────────────────────────
-- HR — Generic tutulma (vergi/sığorta) konfiqurasiya sistemi
--   - hr_deduction_type           : Tutulma növü (GELIR_VERGISI / DSMF / ISH / ITS ...)
--   - hr_deduction_config_version : Versiya (qüvvəyə minmə tarixi ilə, tarixçə üçün)
--   - hr_deduction_bracket        : Hər versiya × tutulma növü × taraf üçün maaş aralıqları
--
-- Hesablama düsturu (motor): sabit_mebleg + (baza − alt_hedd) × faiz − güzəşt
-- Sərhəd məntiqi: alt_hedd < baza ≤ ust_hedd  (ust_hedd NULL = sonsuz)
--
-- QEYD: Köhnə hr_tax_rate_configs cədvəli @Deprecated edilib, məlumat itkisi olmasın
--       deyə bu migration-da SİLİNMİR. Gələcəkdə ayrıca migration ilə təmizlənə bilər.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE hr_deduction_config_version (
    id            BIGSERIAL PRIMARY KEY,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    deleted       BOOLEAN   NOT NULL DEFAULT false,
    deleted_at    TIMESTAMP,
    version_no    INTEGER   NOT NULL,
    effective_date DATE     NOT NULL,
    active        BOOLEAN   NOT NULL DEFAULT false,
    created_by    VARCHAR(255),
    note          VARCHAR(500)
);
CREATE INDEX idx_hr_ded_version_active    ON hr_deduction_config_version(active);
CREATE INDEX idx_hr_ded_version_effective ON hr_deduction_config_version(effective_date);

CREATE TABLE hr_deduction_type (
    id                BIGSERIAL PRIMARY KEY,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL,
    deleted           BOOLEAN   NOT NULL DEFAULT false,
    deleted_at        TIMESTAMP,
    code              VARCHAR(50)  NOT NULL,
    name              VARCHAR(255) NOT NULL,
    applies_to        VARCHAR(20)  NOT NULL,   -- ISCI | ISEGOTUREN | HER_IKISI
    deducted_from_net BOOLEAN      NOT NULL DEFAULT true,
    display_order     INTEGER      NOT NULL DEFAULT 0,
    active            BOOLEAN      NOT NULL DEFAULT true
);
CREATE INDEX idx_hr_ded_type_code ON hr_deduction_type(code);

CREATE TABLE hr_deduction_bracket (
    id                BIGSERIAL PRIMARY KEY,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL,
    deleted           BOOLEAN   NOT NULL DEFAULT false,
    deleted_at        TIMESTAMP,
    version_id        BIGINT NOT NULL REFERENCES hr_deduction_config_version(id),
    deduction_type_id BIGINT NOT NULL REFERENCES hr_deduction_type(id),
    party             VARCHAR(20)    NOT NULL,   -- ISCI | ISEGOTUREN
    lower_bound       NUMERIC(14, 4) NOT NULL DEFAULT 0,
    upper_bound       NUMERIC(14, 4),            -- NULL = sonsuz
    fixed_amount      NUMERIC(14, 4) NOT NULL DEFAULT 0,
    rate              NUMERIC(8, 6)  NOT NULL DEFAULT 0,
    sort_order        INTEGER        NOT NULL DEFAULT 0
    -- güzəşt (exemption): YAGNI — 2026 dəyərləri üçün lazım deyil, motor düsturu
    --   dəstəkləyir (default 0). Lazım olduqda buraya `exemption NUMERIC(14,4)` əlavə edilə bilər.
);
CREATE INDEX idx_hr_ded_bracket_version ON hr_deduction_bracket(version_id);
CREATE INDEX idx_hr_ded_bracket_type    ON hr_deduction_bracket(deduction_type_id);

-- ── Seed: 2026 ilkin versiya (qeyri-neft-qaz / özəl sektor) ──
INSERT INTO hr_deduction_config_version (created_at, updated_at, deleted, version_no, effective_date, active, created_by, note)
VALUES (now(), now(), false, 1, DATE '2026-01-01', true, 'system',
        '2026 ilkin konfiqurasiya — qeyri-neft-qaz, özəl sektor. Rəsmi əməkhaqqı cədvəlinə əsasən.');

INSERT INTO hr_deduction_type (created_at, updated_at, deleted, code, name, applies_to, deducted_from_net, display_order, active) VALUES
(now(), now(), false, 'GELIR_VERGISI', 'Gəlir vergisi',                'ISCI',      true, 1, true),
(now(), now(), false, 'DSMF',          'DSMF / Pensiya Fondu',         'HER_IKISI', true, 2, true),
(now(), now(), false, 'ISH',           'İşsizlikdən sığorta haqqı',    'HER_IKISI', true, 3, true),
(now(), now(), false, 'ITS',           'İcbari tibbi sığorta',         'HER_IKISI', true, 4, true);

-- İTS işəgötürən: hələlik işçi ilə eyni qəbul edilib.
-- DİQQƏT: mənbə cədvəlin başlığında "İTS 1%" göstərilib, amma faktiki düstur işçi ilə eynidir
--         (≤2500: 2%, >2500: 50+(baza−2500)×0.5%). İşəgötürən dərəcəsi HR ilə TƏSDİQLƏNMƏLİDİR.

INSERT INTO hr_deduction_bracket
  (created_at, updated_at, deleted, version_id, deduction_type_id, party, lower_bound, upper_bound, fixed_amount, rate, sort_order)
VALUES
-- Gəlir vergisi (işçi): ≤200→0, ≤2500→(b−200)×3%, ≤8000→75+(b−2500)×10%, >8000→625+(b−8000)×14%
(now(),now(),false,(SELECT id FROM hr_deduction_config_version WHERE version_no=1),(SELECT id FROM hr_deduction_type WHERE code='GELIR_VERGISI'),'ISCI',      0,    200,  0,    0.000000, 1),
(now(),now(),false,(SELECT id FROM hr_deduction_config_version WHERE version_no=1),(SELECT id FROM hr_deduction_type WHERE code='GELIR_VERGISI'),'ISCI',      200,  2500, 0,    0.030000, 2),
(now(),now(),false,(SELECT id FROM hr_deduction_config_version WHERE version_no=1),(SELECT id FROM hr_deduction_type WHERE code='GELIR_VERGISI'),'ISCI',      2500, 8000, 75,   0.100000, 3),
(now(),now(),false,(SELECT id FROM hr_deduction_config_version WHERE version_no=1),(SELECT id FROM hr_deduction_type WHERE code='GELIR_VERGISI'),'ISCI',      8000, NULL, 625,  0.140000, 4),
-- DSMF işçi: ≤200→b×3%, >200→6+(b−200)×10%
(now(),now(),false,(SELECT id FROM hr_deduction_config_version WHERE version_no=1),(SELECT id FROM hr_deduction_type WHERE code='DSMF'),'ISCI',             0,    200,  0,    0.030000, 1),
(now(),now(),false,(SELECT id FROM hr_deduction_config_version WHERE version_no=1),(SELECT id FROM hr_deduction_type WHERE code='DSMF'),'ISCI',             200,  NULL, 6,    0.100000, 2),
-- DSMF işəgötürən: ≤200→b×22%, ≤8000→44+(b−200)×15%, >8000→1214+(b−8000)×11%
(now(),now(),false,(SELECT id FROM hr_deduction_config_version WHERE version_no=1),(SELECT id FROM hr_deduction_type WHERE code='DSMF'),'ISEGOTUREN',       0,    200,  0,    0.220000, 1),
(now(),now(),false,(SELECT id FROM hr_deduction_config_version WHERE version_no=1),(SELECT id FROM hr_deduction_type WHERE code='DSMF'),'ISEGOTUREN',       200,  8000, 44,   0.150000, 2),
(now(),now(),false,(SELECT id FROM hr_deduction_config_version WHERE version_no=1),(SELECT id FROM hr_deduction_type WHERE code='DSMF'),'ISEGOTUREN',       8000, NULL, 1214, 0.110000, 3),
-- İSH işçi: b×0.5%
(now(),now(),false,(SELECT id FROM hr_deduction_config_version WHERE version_no=1),(SELECT id FROM hr_deduction_type WHERE code='ISH'),'ISCI',              0,    NULL, 0,    0.005000, 1),
-- İSH işəgötürən: b×0.5%
(now(),now(),false,(SELECT id FROM hr_deduction_config_version WHERE version_no=1),(SELECT id FROM hr_deduction_type WHERE code='ISH'),'ISEGOTUREN',        0,    NULL, 0,    0.005000, 1),
-- İTS işçi: ≤2500→b×2%, >2500→50+(b−2500)×0.5%
(now(),now(),false,(SELECT id FROM hr_deduction_config_version WHERE version_no=1),(SELECT id FROM hr_deduction_type WHERE code='ITS'),'ISCI',              0,    2500, 0,    0.020000, 1),
(now(),now(),false,(SELECT id FROM hr_deduction_config_version WHERE version_no=1),(SELECT id FROM hr_deduction_type WHERE code='ITS'),'ISCI',              2500, NULL, 50,   0.005000, 2),
-- İTS işəgötürən: işçi ilə eyni (HR ilə təsdiqlənməlidir — yuxarıdakı qeydə bax)
(now(),now(),false,(SELECT id FROM hr_deduction_config_version WHERE version_no=1),(SELECT id FROM hr_deduction_type WHERE code='ITS'),'ISEGOTUREN',        0,    2500, 0,    0.020000, 1),
(now(),now(),false,(SELECT id FROM hr_deduction_config_version WHERE version_no=1),(SELECT id FROM hr_deduction_type WHERE code='ITS'),'ISEGOTUREN',        2500, NULL, 50,   0.005000, 2);
