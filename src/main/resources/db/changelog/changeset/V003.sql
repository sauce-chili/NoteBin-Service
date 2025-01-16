ALTER TABLE note
    ALTER COLUMN create_at SET NOT NULL;

ALTER TABLE note
    ALTER COLUMN is_available SET NOT NULL;

ALTER TABLE note
    ADD COLUMN expire_at timestamptz;

ALTER TABLE note
    ALTER COLUMN expiration_type TYPE VARCHAR(255);

ALTER TABLE note
    ADD CONSTRAINT chk_expiration_type_expire_at
        CHECK (
            NOT (expiration_type = 'BURN_BY_PERIOD' AND expire_at IS NULL)
            );
