-- Smart plug admin/discovery logs use source SMART_PLUG (Java ApplicationLogService.persistSystemLog).

ALTER TABLE monitoring.application_logs
    DROP CONSTRAINT IF EXISTS chk_application_logs_source;

ALTER TABLE monitoring.application_logs
    ADD CONSTRAINT chk_application_logs_source
        CHECK (source IN ('API', 'WORKER', 'BOX', 'EMAIL', 'MONITORING', 'SMART_PLUG'));
