CREATE
EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE monitors_ads
    ADD COLUMN IF NOT EXISTS block_quantity INTEGER NOT NULL DEFAULT 1;

ALTER TABLE monitors_ads
    ALTER COLUMN block_quantity DROP DEFAULT;

CREATE INDEX IF NOT EXISTS idx_monitors_ads_monitor_id_ad_id
    ON monitors_ads (monitor_id, ad_id);

CREATE INDEX IF NOT EXISTS idx_ads_approved_validation
    ON ads (id)
    WHERE validation = 'APPROVED';

CREATE INDEX IF NOT EXISTS idx_ads_name_trgm
    ON ads USING gin (lower (name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_clients_business_name_trgm
    ON clients USING gin (lower (business_name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_ads_client_id
    ON ads (client_id);