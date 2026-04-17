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
        'HEARTBEAT_NEVER_SEEN'
        ));

CREATE TABLE IF NOT EXISTS admin_email_alert_preferences
(
    id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    client_id       UUID                NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    alert_category  VARCHAR(64)         NOT NULL,
    enabled         BOOLEAN             NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_admin_email_alert_pref UNIQUE (client_id, alert_category),
    CONSTRAINT chk_admin_email_alert_category CHECK (alert_category IN (
        'BOX_HEARTBEAT_CONNECTIVITY',
        'SMART_PLUG_UNREACHABLE_OR_POWER',
        'SMART_PLUG_RELAY_OFF',
        'HOST_REBOOT'
        ))
);

CREATE INDEX IF NOT EXISTS idx_admin_email_alert_prefs_client ON admin_email_alert_preferences (client_id);
