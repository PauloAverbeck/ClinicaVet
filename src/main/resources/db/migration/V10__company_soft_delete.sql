-- V10__company_soft_delete.sql
-- Implementa soft delete em company usando coluna deleted_at

-- 1) Adiciona coluna deleted_at (se ainda não existir)
ALTER TABLE company
    ADD COLUMN deleted_at timestamp without time zone;

-- 2) Se existir coluna is_active antiga, migra dados e remove
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_name = 'company'
           AND column_name = 'is_active'
    ) THEN
        -- Registra como "deletada" qualquer empresa com is_active = false
        UPDATE company
           SET deleted_at = now()
         WHERE is_active = false
           AND deleted_at IS NULL;

        ALTER TABLE company
            DROP COLUMN is_active;
    END IF;
END $$;

-- 3) Remove índices únicos antigos, se existirem
DROP INDEX IF EXISTS uq_company_document;
DROP INDEX IF EXISTS uq_company_document_is_active;

-- 4) Cria índice único novo considerando apenas empresas "ativas"
-- (sem deleted_at)
CREATE UNIQUE INDEX uq_company_document_active
    ON company (document_type, document)
    WHERE deleted_at IS NULL;