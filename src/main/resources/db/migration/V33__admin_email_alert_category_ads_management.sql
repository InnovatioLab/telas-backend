ALTER TABLE admin_email_alert_preferences
    DROP CONSTRAINT IF EXISTS chk_admin_email_alert_category;

ALTER TABLE admin_email_alert_preferences
    ADD CONSTRAINT chk_admin_email_alert_category CHECK (alert_category IN (
        'BOX_HEARTBEAT_CONNECTIVITY',
        'SMART_PLUG_UNREACHABLE_OR_POWER',
        'SMART_PLUG_RELAY_OFF',
        'HOST_REBOOT',
        'ADS_MANAGEMENT'
        ));

