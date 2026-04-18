INSERT INTO client_granted_permissions (id, client_id, permission_code, granted_at, granted_by_client_id)
SELECT gen_random_uuid(), cgp.client_id, 'MONITORING_BOX_PING_VIEW', NOW() AT TIME ZONE 'UTC', NULL
FROM client_granted_permissions cgp
WHERE cgp.permission_code = 'MONITORING_TESTING_VIEW'
  AND NOT EXISTS (
    SELECT 1
    FROM client_granted_permissions x
    WHERE x.client_id = cgp.client_id
      AND x.permission_code = 'MONITORING_BOX_PING_VIEW'
  );
