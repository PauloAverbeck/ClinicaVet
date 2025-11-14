ALTER TABLE user_company
    ALTER COLUMN created_by_user_id DROP NOT NULL;

ALTER TABLE user_company
    DROP CONSTRAINT IF EXISTS user_company_user_id_fkey;

ALTER TABLE user_company
    DROP CONSTRAINT IF EXISTS user_company_created_by_user_id_fkey;

ALTER TABLE user_company
    ADD CONSTRAINT user_company_user_id_fkey
    FOREIGN KEY (user_id)
    REFERENCES app_user(id)
    ON DELETE CASCADE;

ALTER TABLE user_company
    ADD CONSTRAINT user_company_created_by_user_id_fkey
    FOREIGN KEY (created_by_user_id)
    REFERENCES app_user(id)
    ON DELETE SET NULL;