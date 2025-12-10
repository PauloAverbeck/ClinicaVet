DROP INDEX IF EXISTS uq_company_document_active;
DROP INDEX IF EXISTS uq_company_document;

ALTER TABLE company
    DROP CONSTRAINT IF EXISTS unique_company_document_type,
    DROP CONSTRAINT IF EXISTS unique_company_document;

ALTER TABLE company
    ADD CONSTRAINT unique_company_document
    UNIQUE (document_type, document);