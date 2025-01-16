ALTER TABLE note
    ALTER COLUMN title SET NOT NULL;

ALTER TABLE note
    alter column title set default '';

alter table note
    drop column expire_at;

alter table note
    add column expiration_period interval;

alter table note
    add column expiration_from timestamptz;

SELECT conname
FROM pg_constraint
WHERE conname = 'chk_expiration_type_expire_at';


ALTER TABLE note
    ADD CONSTRAINT chk_expiration_type_expire_period_expiration_from
        CHECK (
            NOT (expiration_type = 'BURN_BY_PERIOD' AND (expiration_period IS NULL OR expiration_from IS NULL))
            );

create index idx_url on note (url);
