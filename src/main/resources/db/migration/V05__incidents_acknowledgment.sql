ALTER TABLE monitoring.incidents
    ADD COLUMN acknowledged_at TIMESTAMPTZ NULL,
    ADD COLUMN acknowledge_reason TEXT NULL,
    ADD COLUMN acknowledged_by UUID NULL REFERENCES public.clients (id) ON DELETE SET NULL;

CREATE INDEX idx_incidents_acknowledged_at ON monitoring.incidents (acknowledged_at)
    WHERE acknowledged_at IS NOT NULL;
