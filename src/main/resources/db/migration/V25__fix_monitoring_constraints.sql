-- Ensure monitoring constraints accept current enum-like values.
-- Some environments may have been created before V12/V15/V19/V22 or had partial migrations applied.

ALTER TABLE monitoring.application_logs
    DROP CONSTRAINT IF EXISTS chk_application_logs_source;

ALTER TABLE monitoring.application_logs
    ADD CONSTRAINT chk_application_logs_source
        CHECK (source IN ('API', 'WORKER', 'BOX', 'EMAIL', 'MONITORING'));

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

