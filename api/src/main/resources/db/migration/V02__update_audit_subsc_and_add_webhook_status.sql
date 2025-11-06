ALTER TABLE subscriptions
    ADD COLUMN version        INTEGER DEFAULT 1;

ALTER TABLE payments
    ADD COLUMN version        INTEGER DEFAULT 1;

ALTER TABLE  subscriptions_aud
    ADD COLUMN  started_at      TIMESTAMP WITH TIME ZONE,
    ADD COLUMN ends_at         TIMESTAMP WITH TIME ZONE;

ALTER TABLE webhook_events
    ADD COLUMN status          VARCHAR(20) DEFAULT 'PROCESSED';