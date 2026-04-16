ALTER TABLE monitoring.application_logs
    DROP CONSTRAINT chk_application_logs_source;

ALTER TABLE monitoring.application_logs
    ADD CONSTRAINT chk_application_logs_source
        CHECK (source IN ('API', 'WORKER', 'BOX', 'EMAIL'));
