ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS fl_cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE subscriptions_aud
    ADD COLUMN IF NOT EXISTS fl_cancel_at_period_end BOOLEAN;

CREATE INDEX IF NOT EXISTS idx_subscriptions_cancel_at_period_end
    ON subscriptions (client_id, fl_cancel_at_period_end, status);

