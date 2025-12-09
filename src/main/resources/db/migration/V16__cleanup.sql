-- V16__cleanup.sql
-- Limpeza de constraints/índices e remoção de tabelas legadas

-- =========================================================
--  1) USER_COMPANY: unicidade apenas para vínculos ATIVOS
--     + limpeza de índices e FKs antigas
-- =========================================================

-- Remove a constraint antiga que bloqueia qualquer duplicata,
-- mesmo quando deleted_at NÃO é null.
ALTER TABLE user_company
    DROP CONSTRAINT IF EXISTS uq_user_company;

-- Garante que exista APENAS unicidade para vínculos ATIVOS
-- (deleted_at IS NULL).
CREATE UNIQUE INDEX IF NOT EXISTS uq_uc_user_company_active
    ON user_company(user_id, company_id)
    WHERE deleted_at IS NULL;

-- Remove índices antigos/determinísticos em duplicidade.
DROP INDEX IF EXISTS idx_user_company_user_active;
DROP INDEX IF EXISTS idx_user_company_company_active;
-- Mantemos os índices criados em V8 (idx_uc_user_active / idx_uc_company_active).

-- Corrige FKs duplicadas na user_company:
-- Em V7 foram criadas fk_uc_user / fk_uc_created_by_user,
-- e em V9 foram criadas novas FKs com outros nomes.
-- Para que as FKs de V9 tenham efeito (ON DELETE CASCADE / SET NULL),
-- removemos as antigas.
ALTER TABLE user_company
    DROP CONSTRAINT IF EXISTS fk_uc_user,
    DROP CONSTRAINT IF EXISTS fk_uc_created_by_user;


-- =========================================================
--  2) COMPANY: unicidade apenas para empresas ATIVAS
-- =========================================================

-- Remove a constraint antiga que ainda força unicidade global
-- (sem considerar deleted_at).
ALTER TABLE company
    DROP CONSTRAINT IF EXISTS unique_company_document_type;

-- Garante índice único apenas para empresas "ativas"
-- (sem deleted_at), conforme V10.
CREATE UNIQUE INDEX IF NOT EXISTS uq_company_document_active
    ON company (document_type, document)
    WHERE deleted_at IS NULL;


-- =========================================================
--  3) CLIENT / PET: triggers de update_date
-- =========================================================

-- Atualiza automaticamente update_date em client
DROP TRIGGER IF EXISTS set_client_updated_at ON client;
CREATE TRIGGER set_client_updated_at
BEFORE UPDATE ON client
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- Atualiza automaticamente update_date em pet
DROP TRIGGER IF EXISTS set_pet_updated_at ON pet;
CREATE TRIGGER set_pet_updated_at
BEFORE UPDATE ON pet
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();


-- =========================================================
--  4) Remoção de tabelas legadas não utilizadas
-- =========================================================

-- Tabela de exemplo/legado não utilizada no domínio da clínica
DROP TABLE IF EXISTS task CASCADE;

-- Modelo antigo person/animal substituído por client/pet
DROP TABLE IF EXISTS animal CASCADE;
DROP TABLE IF EXISTS person CASCADE;