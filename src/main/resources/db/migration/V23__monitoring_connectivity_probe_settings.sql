CREATE TABLE monitoring.connectivity_probe_settings
(
    id                              SMALLINT PRIMARY KEY CHECK (id = 1),
    box_connectivity_probe_interval_ms BIGINT NOT NULL CHECK (box_connectivity_probe_interval_ms >= 5000
        AND box_connectivity_probe_interval_ms <= 86400000)
);

INSERT INTO monitoring.connectivity_probe_settings (id, box_connectivity_probe_interval_ms)
VALUES (1, 10000);

INSERT INTO client_granted_permissions (id, client_id, permission_code, granted_at, granted_by_client_id)
SELECT gen_random_uuid(), cgp.client_id, 'MONITORING_CONNECTIVITY_PROBE_SETTINGS', NOW() AT TIME ZONE 'UTC', NULL
FROM client_granted_permissions cgp
WHERE cgp.permission_code = 'MONITORING_BOX_PING_VIEW'
  AND NOT EXISTS (
    SELECT 1
    FROM client_granted_permissions x
    WHERE x.client_id = cgp.client_id
      AND x.permission_code = 'MONITORING_CONNECTIVITY_PROBE_SETTINGS'
  );
