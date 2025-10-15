DELETE FROM app_user;

INSERT INTO app_user (id, email, name, password_hash, version)
VALUES (1, 'teste@exemplo.com', 'Teste',
        '$2a$10$KbQi8a8iJ3o8Q1z6fU5eeO6e9gq7Hqg6s2v7wGmH1Yq5mQxT1Gcz2', -- bcrypt válido
        0);

UPDATE app_user
SET reset_token = 'tokentest',
    reset_token_expiry = DATEADD('MINUTE', 30, CURRENT_TIMESTAMP);