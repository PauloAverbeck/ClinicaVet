ALTER TABLE client
    ADD COLUMN person_type      VARCHAR(20) NULL,
    ADD COLUMN doc_type         VARCHAR(20) NULL,
    ADD COLUMN document         VARCHAR(50) NULL,
    ADD COLUMN created_by_user_id BIGINT NULL REFERENCES app_user(id),
    ADD COLUMN cep              VARCHAR(20) NULL,
    ADD COLUMN uf               VARCHAR(2)  NULL,
    ADD COLUMN city             VARCHAR(100) NULL,
    ADD COLUMN district         VARCHAR(100) NULL,
    ADD COLUMN street           VARCHAR(150) NULL,
    ADD COLUMN number           VARCHAR(20) NULL,
    ADD COLUMN complement       VARCHAR(150) NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_client_company_doc
    ON client (company_id, doc_type, document)
    WHERE deleted_at IS NULL
      AND doc_type IS NOT NULL
      AND document IS NOT NULL;