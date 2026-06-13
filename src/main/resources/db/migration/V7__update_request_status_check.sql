-- ============================================================================
-- V7: RequestStatus enum üçün CHECK constraint-ləri yenilə
-- ============================================================================
-- V1-də yaradılmış constraint-lər yalnız köhnə 6 status dəyərini qəbul edirdi.
-- Yeni 14 dəyərlik flow üçün constraint-ləri drop edib yenidən yaradırıq.
-- ============================================================================

ALTER TABLE tech_requests
    DROP CONSTRAINT IF EXISTS tech_requests_status_check;

ALTER TABLE tech_requests
    ADD CONSTRAINT tech_requests_status_check
    CHECK (status IN (
        'DRAFT',
        'PENDING',
        'PM_REVIEW',
        'PM_SHORTLIST_READY',
        'COORDINATOR_NEGOTIATING',
        'COORDINATOR_PROPOSED',
        'PM_PRICE_NEGOTIATION',
        'PM_APPROVED',
        'ACCOUNTING_DOCS_CHECK',
        'EXECUTION_READY',
        'OPERATOR_ASSIGNED',
        'EQUIPMENT_DISPATCHED',
        'DELIVERED',
        'REJECTED'
    ));

ALTER TABLE request_status_logs
    DROP CONSTRAINT IF EXISTS request_status_logs_old_status_check;

ALTER TABLE request_status_logs
    ADD CONSTRAINT request_status_logs_old_status_check
    CHECK (old_status IN (
        'DRAFT',
        'PENDING',
        'PM_REVIEW',
        'PM_SHORTLIST_READY',
        'COORDINATOR_NEGOTIATING',
        'COORDINATOR_PROPOSED',
        'PM_PRICE_NEGOTIATION',
        'PM_APPROVED',
        'ACCOUNTING_DOCS_CHECK',
        'EXECUTION_READY',
        'OPERATOR_ASSIGNED',
        'EQUIPMENT_DISPATCHED',
        'DELIVERED',
        'REJECTED'
    ));

ALTER TABLE request_status_logs
    DROP CONSTRAINT IF EXISTS request_status_logs_new_status_check;

ALTER TABLE request_status_logs
    ADD CONSTRAINT request_status_logs_new_status_check
    CHECK (new_status IN (
        'DRAFT',
        'PENDING',
        'PM_REVIEW',
        'PM_SHORTLIST_READY',
        'COORDINATOR_NEGOTIATING',
        'COORDINATOR_PROPOSED',
        'PM_PRICE_NEGOTIATION',
        'PM_APPROVED',
        'ACCOUNTING_DOCS_CHECK',
        'EXECUTION_READY',
        'OPERATOR_ASSIGNED',
        'EQUIPMENT_DISPATCHED',
        'DELIVERED',
        'REJECTED'
    ));

-- shortlist_items.party_type üçün constraint (yeni cədvəl, V6-da yaradılıb,
-- amma Hibernate-in özü-özünə yaradacağı constraint olduqda yenidən təsdiqləyirik)
ALTER TABLE shortlist_items
    DROP CONSTRAINT IF EXISTS shortlist_items_party_type_check;

ALTER TABLE shortlist_items
    ADD CONSTRAINT shortlist_items_party_type_check
    CHECK (party_type IN ('CONTRACTOR', 'INVESTOR'));

-- request_documents.doc_type üçün constraint
ALTER TABLE request_documents
    DROP CONSTRAINT IF EXISTS request_documents_doc_type_check;

ALTER TABLE request_documents
    ADD CONSTRAINT request_documents_doc_type_check
    CHECK (doc_type IN ('CONTRACT', 'PRICE_PROTOCOL'));
