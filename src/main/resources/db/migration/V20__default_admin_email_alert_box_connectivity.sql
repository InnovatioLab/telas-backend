INSERT INTO admin_email_alert_preferences (id, client_id, alert_category, enabled)
SELECT gen_random_uuid(), c.id, 'BOX_HEARTBEAT_CONNECTIVITY', true
FROM clients c
WHERE c.role = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM admin_email_alert_preferences p
    WHERE p.client_id = c.id
      AND p.alert_category = 'BOX_HEARTBEAT_CONNECTIVITY'
  );
