-- =========================================================
--  DATASET DE TESTE PARA ClinicaVet (H2)
-- =========================================================

-- Limpeza
DELETE FROM attendance;
DELETE FROM animal;
DELETE FROM person;
DELETE FROM user_company;
DELETE FROM company;
DELETE FROM app_user;

-- =========================================================
--  Usuários
-- =========================================================
INSERT INTO app_user (id, name, email, password_hash, version, creation_date, update_date)
VALUES
  (1, 'Administrador', 'admin@clinica.com', '$2a$10$7rOq2b0xZZZhashdeadminxxx', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'Usuário Normal', 'user@clinica.com', '$2a$10$7rOq2b0xYYYhashdeuserxxx', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =========================================================
--  Empresa
-- =========================================================
INSERT INTO company (id, name, document_type, document, version, creation_date, update_date)
VALUES
  (1, 'Clínica São Francisco', 'CNPJ', '12.345.678/0001-90', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'Pet Center Floripa',     'CNPJ', '98.765.432/0001-10', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =========================================================
--  Vínculo usuário–empresa
-- =========================================================
INSERT INTO user_company (id, user_id, company_id, admin, created_by_user_id, version, creation_date, update_date)
VALUES
  (1, 1, 1, TRUE, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 2, 1, FALSE, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =========================================================
--  Pessoas
-- =========================================================
INSERT INTO person (id, company_id, person_type, document_type, document, name, phone, email, version, creation_date, update_date)
VALUES
  (1, 1, 'FISICA', 'CPF', '123.456.789-00', 'João Tutor', '(48) 99999-8888', 'joao@exemplo.com', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 1, 'FISICA', 'CPF', '987.654.321-00', 'Maria Dona', '(48) 98888-7777', 'maria@exemplo.com', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =========================================================
--  Animais
-- =========================================================
INSERT INTO animal (id, tutor_id, name, animal_type, note, version, creation_date, update_date)
VALUES
  (1, 1, 'Rex', 'CACHORRO', 'Pastor alemão', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 2, 'Mimi', 'GATO', 'Gato persa branco', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =========================================================
--  Atendimentos (simples)
-- =========================================================
INSERT INTO attendance (id, animal_id, scheduled_at, description, version, creation_date, update_date)
VALUES
  (1, 1, DATEADD('DAY', 1, CURRENT_TIMESTAMP), 'Consulta de rotina', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 2, DATEADD('DAY', 2, CURRENT_TIMESTAMP), 'Revisão vacinas', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);