ALTER TABLE ad_requests
    ADD COLUMN IF NOT EXISTS target_monitor_id UUID NULL,
    ADD COLUMN IF NOT EXISTS submission_mode VARCHAR(32) NULL,
    ADD COLUMN IF NOT EXISTS request_origin VARCHAR(16) NOT NULL DEFAULT 'CLIENT';

ALTER TABLE ad_requests
    ADD CONSTRAINT fk_ad_requests_target_monitor
        FOREIGN KEY (target_monitor_id) REFERENCES monitors (id) ON DELETE SET NULL;

UPDATE ad_requests
SET request_origin = 'CLIENT'
WHERE request_origin IS NULL OR TRIM(request_origin) = '';

CREATE INDEX IF NOT EXISTS idx_ad_requests_partner_active
    ON ad_requests (client_id, request_origin, submission_mode)
    WHERE active = true AND request_origin = 'PARTNER';
