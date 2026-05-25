ALTER TABLE ads
    ADD COLUMN IF NOT EXISTS partner_removal_requested_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE ads
    ADD COLUMN IF NOT EXISTS partner_removal_message TEXT;
