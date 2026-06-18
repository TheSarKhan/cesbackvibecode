-- ════════════════════════════════════════════════════════════════════════════
-- V26 — Podratçı & İnvestor sənəd bölmələri
-- Müştəridə olduğu kimi, podratçı və investor profillərinə də əl ilə sənəd
-- yükləmə imkanı (VÖEN, dövlət qeydiyyatı, bank rekvizit, çərçivə müqaviləsi və s.).
-- Bu cədvəllər yalnız ƏL İLƏ yüklənən sənədləri saxlayır; layihə müqavilələri,
-- təhvil aktları, texnika sənədləri və qaimələr öz cədvəllərində qalır və profil
-- "sənəd mərkəzi"ndə aqreqasiya ilə göstərilir.
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE contractor_documents (
    id             BIGSERIAL PRIMARY KEY,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL,
    deleted        BOOLEAN   NOT NULL DEFAULT false,
    deleted_at     TIMESTAMP,
    contractor_id  BIGINT       NOT NULL REFERENCES contractors (id),
    file_path      VARCHAR(255) NOT NULL,
    document_name  VARCHAR(255),
    file_type      VARCHAR(50),
    document_date  DATE,
    uploaded_by    BIGINT REFERENCES users (id)
);

CREATE INDEX idx_contractor_docs_contractor ON contractor_documents (contractor_id);

CREATE TABLE investor_documents (
    id             BIGSERIAL PRIMARY KEY,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL,
    deleted        BOOLEAN   NOT NULL DEFAULT false,
    deleted_at     TIMESTAMP,
    investor_id    BIGINT       NOT NULL REFERENCES investors (id),
    file_path      VARCHAR(255) NOT NULL,
    document_name  VARCHAR(255),
    file_type      VARCHAR(50),
    document_date  DATE,
    uploaded_by    BIGINT REFERENCES users (id)
);

CREATE INDEX idx_investor_docs_investor ON investor_documents (investor_id);
