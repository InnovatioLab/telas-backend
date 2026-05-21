INSERT INTO client_granted_permissions (id, client_id, permission_code, granted_at, granted_by_client_id)
SELECT gen_random_uuid(), cgp.client_id, 'ADMIN_ADS_BOX_DISPATCH_TO_SCREEN', NOW() AT TIME ZONE 'UTC', NULL
FROM client_granted_permissions cgp
WHERE cgp.permission_code = 'ADMIN_ADS_MANAGE'
  AND NOT EXISTS (
    SELECT 1
    FROM client_granted_permissions x
    WHERE x.client_id = cgp.client_id
      AND x.permission_code = 'ADMIN_ADS_BOX_DISPATCH_TO_SCREEN'
  );
