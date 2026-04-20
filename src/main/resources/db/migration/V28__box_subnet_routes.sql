CREATE TABLE IF NOT EXISTS monitoring.box_subnet_routes
(
    id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    box_id          UUID                NOT NULL REFERENCES public.boxes (id) ON DELETE CASCADE,
    cidr            VARCHAR(64)         NOT NULL,
    source          VARCHAR(32)         NOT NULL,
    advertised      BOOLEAN             NOT NULL DEFAULT FALSE,
    enabled_route   BOOLEAN             NOT NULL DEFAULT FALSE,
    last_synced_at  TIMESTAMPTZ         NOT NULL DEFAULT (now()),
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT (now()),
    updated_at      TIMESTAMPTZ         NOT NULL DEFAULT (now()),
    CONSTRAINT chk_box_subnet_routes_source CHECK (source IN ('TAILSCALE'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_box_subnet_routes_box_cidr
    ON monitoring.box_subnet_routes (box_id, cidr);

CREATE INDEX IF NOT EXISTS idx_box_subnet_routes_box
    ON monitoring.box_subnet_routes (box_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON monitoring.box_subnet_routes TO telas_app;
