ALTER TABLE client
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT,
    ADD CONSTRAINT fk_client_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES app_user(id);

ALTER TABLE pet
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT,
    ADD CONSTRAINT fk_pet_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES app_user(id);