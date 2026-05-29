UPDATE ad_requests ar
SET target_monitor_id = NULL
FROM ads a
WHERE a.ad_request_id = ar.id
  AND ar.target_monitor_id IS NOT NULL
  AND (
    a.on_air_notified_at IS NOT NULL
    OR a.partner_box_staged_at IS NOT NULL
  )
  AND NOT EXISTS (
    SELECT 1
    FROM monitors_ads ma
    WHERE ma.ad_id = a.id
      AND ma.monitor_id = ar.target_monitor_id
  );
