ALTER TABLE monitoring.smart_plugs
    ADD COLUMN box_id UUID NULL REFERENCES public.boxes (id) ON DELETE SET NULL;

CREATE UNIQUE INDEX uq_smart_plugs_box_assigned
    ON monitoring.smart_plugs (box_id)
    WHERE box_id IS NOT NULL;

ALTER TABLE monitoring.smart_plugs
    ADD CONSTRAINT chk_smart_plugs_monitor_xor_box CHECK (
        NOT (monitor_id IS NOT NULL AND box_id IS NOT NULL)
        );
