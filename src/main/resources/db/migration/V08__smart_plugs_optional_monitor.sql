ALTER TABLE monitoring.smart_plugs
    DROP CONSTRAINT IF EXISTS smart_plugs_monitor_id_fkey;

ALTER TABLE monitoring.smart_plugs
    DROP CONSTRAINT IF EXISTS smart_plugs_monitor_id_key;

ALTER TABLE monitoring.smart_plugs
    ALTER COLUMN monitor_id DROP NOT NULL;

ALTER TABLE monitoring.smart_plugs
    ADD CONSTRAINT smart_plugs_monitor_id_fkey
        FOREIGN KEY (monitor_id) REFERENCES public.monitors (id) ON DELETE SET NULL;

CREATE UNIQUE INDEX uq_smart_plugs_monitor_assigned
    ON monitoring.smart_plugs (monitor_id)
    WHERE monitor_id IS NOT NULL;
