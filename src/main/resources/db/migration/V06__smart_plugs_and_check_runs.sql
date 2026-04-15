CREATE TABLE monitoring.smart_plugs
(
    id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    mac_address     VARCHAR(32)         NOT NULL,
    vendor          VARCHAR(32)         NOT NULL,
    model           VARCHAR(128) NULL,
    display_name    VARCHAR(255) NULL,
    monitor_id      UUID                NOT NULL UNIQUE REFERENCES public.monitors (id) ON DELETE CASCADE,
    enabled         BOOLEAN             NOT NULL DEFAULT TRUE,
    last_seen_ip    VARCHAR(45) NULL,
    account_email   VARCHAR(255) NULL,
    password_cipher TEXT NULL,
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT (now()),
    updated_at      TIMESTAMPTZ         NOT NULL DEFAULT (now()),
    CONSTRAINT chk_smart_plugs_vendor CHECK (vendor IN ('KASA', 'TAPO', 'TPLINK'))
);

CREATE UNIQUE INDEX uq_smart_plugs_mac ON monitoring.smart_plugs (mac_address);

CREATE INDEX idx_smart_plugs_monitor ON monitoring.smart_plugs (monitor_id);
CREATE INDEX idx_smart_plugs_enabled ON monitoring.smart_plugs (enabled) WHERE enabled = TRUE;

ALTER TABLE monitoring.check_runs
    ALTER COLUMN check_definition_id DROP NOT NULL;

ALTER TABLE monitoring.check_runs
    ADD COLUMN smart_plug_id UUID NULL REFERENCES monitoring.smart_plugs (id) ON DELETE SET NULL;

ALTER TABLE monitoring.check_runs
    ADD CONSTRAINT chk_check_runs_definition_or_plug CHECK (
        check_definition_id IS NOT NULL OR smart_plug_id IS NOT NULL
        );
