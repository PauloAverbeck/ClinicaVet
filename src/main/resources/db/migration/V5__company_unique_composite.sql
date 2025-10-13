ALTER TABLE company
    DROP CONSTRAINT IF EXISTS unique_company_document;

ALTER TABLE company
    ADD CONSTRAINT unique_company_document_type
    UNIQUE (document_type, document);