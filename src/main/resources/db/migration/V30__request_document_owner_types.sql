-- ════════════════════════════════════════════════════════════════════════════
-- V30 — request_documents.doc_type CHECK constraint-ini sahib tərəfi tipləri ilə yenilə
-- Problem: V7-də yaradılan request_documents_doc_type_check yalnız
-- ('CONTRACT','PRICE_PROTOCOL')-a icazə verirdi. Sonradan sahib tərəfi (podratçı/
-- investor) sənədləri üçün OWNER_CONTRACT və OWNER_PRICE_PROTOCOL tipləri əlavə
-- olundu (RequestDocumentType enum), amma CHECK yenilənmədi. Nəticədə mühasibatlıq
-- sənəd yoxlanışında "Sahib tərəfi" faylı yüklədikdə INSERT bu CHECK-i pozurdu
-- -> "Bu məlumat artıq mövcuddur və ya məhdudiyyəti pozur" xətası.
-- ════════════════════════════════════════════════════════════════════════════

ALTER TABLE request_documents
    DROP CONSTRAINT IF EXISTS request_documents_doc_type_check;

ALTER TABLE request_documents
    ADD CONSTRAINT request_documents_doc_type_check
    CHECK (doc_type IN ('CONTRACT', 'PRICE_PROTOCOL', 'OWNER_CONTRACT', 'OWNER_PRICE_PROTOCOL'));
