-- 1) user_company: não permitir dois vínculos ATIVOS iguais
CREATE UNIQUE INDEX IF NOT EXISTS uq_uc_user_company_active
  ON user_company(user_id, company_id)
  WHERE deleted_at IS NULL;

-- 2) Acelerar consultas comuns
CREATE INDEX IF NOT EXISTS idx_uc_user_active
  ON user_company(user_id)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_uc_company_active
  ON user_company(company_id)
  WHERE deleted_at IS NULL;

-- 3) company: documento único por tipo (evita duplicidade em corrida)
CREATE UNIQUE INDEX IF NOT EXISTS uq_company_document
  ON company(document_type, document);