UPDATE ads a
SET ad_request_id = NULL
FROM (
    SELECT ad_request_id
    FROM ads
    WHERE ad_request_id IS NOT NULL
    GROUP BY ad_request_id
    HAVING COUNT(*) > 1
) dup
WHERE a.ad_request_id = dup.ad_request_id
  AND a.id NOT IN (
    SELECT DISTINCT ON (ad_request_id) id
    FROM ads
    WHERE ad_request_id = dup.ad_request_id
    ORDER BY ad_request_id, created_at DESC NULLS LAST, id DESC
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ads_ad_request_id
    ON ads (ad_request_id)
    WHERE ad_request_id IS NOT NULL;
