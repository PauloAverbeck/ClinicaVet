DROP INDEX IF EXISTS index_app_user_reset_token;

ALTER TABLE app_user
    DROP COLUMN IF EXISTS reset_token,
    DROP COLUMN IF EXISTS reset_token_expiry;