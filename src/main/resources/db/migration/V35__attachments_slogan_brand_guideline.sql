ALTER TABLE attachments
    ADD COLUMN IF NOT EXISTS slogan VARCHAR(50);
ALTER TABLE attachments
    ADD COLUMN IF NOT EXISTS brand_guideline_url TEXT;
