ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS cancel_requested_at TIMESTAMP WITHOUT TIME ZONE NULL,
    ADD COLUMN IF NOT EXISTS cancel_at_period_end_at TIMESTAMP WITHOUT TIME ZONE NULL;

ALTER TABLE subscriptions_aud
    ADD COLUMN IF NOT EXISTS cancel_requested_at TIMESTAMP WITHOUT TIME ZONE NULL,
    ADD COLUMN IF NOT EXISTS cancel_at_period_end_at TIMESTAMP WITHOUT TIME ZONE NULL;

CREATE INDEX IF NOT EXISTS idx_subscriptions_cancel_dates
    ON subscriptions (client_id, cancel_requested_at, cancel_at_period_end_at, status);

