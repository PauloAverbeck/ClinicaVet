ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS reset_token VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS reset_token_expiry TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS index_app_user_reset_token ON app_user (reset_token);
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_email_lower ON app_user (LOWER(email));