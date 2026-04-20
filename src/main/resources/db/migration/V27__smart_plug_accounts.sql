CREATE TABLE IF NOT EXISTS monitoring.smart_plug_accounts
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    box_id          UUID         NOT NULL REFERENCES public.boxes (id) ON DELETE CASCADE,
    vendor          VARCHAR(32)  NOT NULL,
    account_email   VARCHAR(255) NULL,
    password_cipher TEXT         NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT (now()),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT (now()),
    CONSTRAINT chk_smart_plug_accounts_vendor CHECK (vendor IN ('KASA', 'TAPO', 'TPLINK'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_smart_plug_accounts_box_vendor
    ON monitoring.smart_plug_accounts (box_id, vendor);

ALTER TABLE monitoring.smart_plugs
    ADD COLUMN IF NOT EXISTS smart_plug_account_id UUID NULL
        REFERENCES monitoring.smart_plug_accounts (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_smart_plugs_account
    ON monitoring.smart_plugs (smart_plug_account_id);

