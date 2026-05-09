ALTER TABLE ads
    ADD COLUMN IF NOT EXISTS source_attachment_id UUID;

ALTER TABLE ads
    ADD CONSTRAINT fk_ads_source_attachment
        FOREIGN KEY (source_attachment_id) REFERENCES attachments (id)
        ON UPDATE NO ACTION ON DELETE SET NULL;
