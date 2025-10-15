DELETE FROM app_user;

INSERT INTO app_user (id, email, name, password_hash, prov_pw_hash, version, creation_date, update_date)
VALUES (1, 'teste@exemplo.com', 'Teste', '$2a$10$7rOq2b0x', NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

UPDATE app_user
SET reset_token = 'tokentest',
    reset_token_expiry = DATEADD('MINUTE', 30, CURRENT_TIMESTAMP)
WHERE id = 1;