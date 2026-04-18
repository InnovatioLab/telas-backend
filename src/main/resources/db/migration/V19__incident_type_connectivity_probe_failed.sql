ALTER TABLE monitoring.incidents
    DROP CONSTRAINT IF EXISTS chk_incidents_type;

ALTER TABLE monitoring.incidents
    ADD CONSTRAINT chk_incidents_type CHECK (incident_type IN (
        'BOX_UNREACHABLE',
        'MONITOR_OFF',
        'POWER_LOSS',
        'DISPLAY_PIPELINE_DOWN',
        'HEARTBEAT_STALE',
        'OTHER',
        'HOST_REBOOT',
        'HEARTBEAT_NEVER_SEEN',
        'CONNECTIVITY_PROBE_FAILED'
        ));
