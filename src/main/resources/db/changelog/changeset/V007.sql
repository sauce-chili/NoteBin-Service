ALTER TABLE note
DROP CONSTRAINT IF EXISTS chk_expiration_type_expire_period_expiration_from;

ALTER TABLE note
    ADD CONSTRAINT chk_expiration_type_expire_period_expiration_from
        CHECK (
            NOT (
                expiration_type = 'BURN_BY_PERIOD' AND
                (expiration_period IS NULL OR expiration_from IS NULL)
                ) AND
            NOT (
                (expiration_type = 'NEVER' OR expiration_type = 'BURN_AFTER_READ')
                    AND (expiration_period IS NOT NULL OR expiration_from IS NOT NULL)
                )
            );