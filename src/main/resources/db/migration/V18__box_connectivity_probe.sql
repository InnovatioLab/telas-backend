CREATE TABLE monitoring.box_connectivity_probe
(
    box_id         UUID PRIMARY KEY REFERENCES public.boxes (id) ON DELETE CASCADE,
    last_probe_at  TIMESTAMPTZ NOT NULL,
    reachable      BOOLEAN     NOT NULL,
    probe_detail   TEXT NULL,
    box_ip         VARCHAR(128) NULL,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT (now())
);

CREATE INDEX idx_box_connectivity_probe_last_probe
    ON monitoring.box_connectivity_probe (last_probe_at DESC);
