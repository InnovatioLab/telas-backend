ALTER TABLE ads
    ADD COLUMN IF NOT EXISTS unused_since TIMESTAMPTZ NULL;

ALTER TABLE ads_aud
    ADD COLUMN IF NOT EXISTS unused_since TIMESTAMPTZ NULL;

ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS ads_retention_days_override INTEGER NULL;

ALTER TABLE clients
    DROP CONSTRAINT IF EXISTS chk_clients_ads_retention_days_override;

ALTER TABLE clients
    ADD CONSTRAINT chk_clients_ads_retention_days_override CHECK (
        ads_retention_days_override IS NULL OR ads_retention_days_override > 0
        );

ALTER TABLE clients_aud
    ADD COLUMN IF NOT EXISTS ads_retention_days_override INTEGER NULL;

CREATE TABLE IF NOT EXISTS scheduler_job_runs
(
    id           UUID PRIMARY KEY,
    job_id       VARCHAR(128)             NOT NULL,
    started_at   TIMESTAMPTZ              NOT NULL,
    ended_at     TIMESTAMPTZ              NULL,
    status       VARCHAR(20)              NOT NULL,
    error_message TEXT NULL
);

CREATE INDEX IF NOT EXISTS idx_scheduler_job_runs_job_started
    ON scheduler_job_runs (job_id, started_at DESC);

UPDATE ads
SET unused_since = COALESCE(ads.updated_at, ads.created_at)
WHERE validation = 'APPROVED'
  AND NOT EXISTS (SELECT 1 FROM monitors_ads ma WHERE ma.ad_id = ads.id)
  AND unused_since IS NULL;
