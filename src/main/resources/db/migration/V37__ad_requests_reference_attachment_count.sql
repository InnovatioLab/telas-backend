ALTER TABLE ad_requests
    ADD COLUMN IF NOT EXISTS reference_attachment_count INTEGER NOT NULL DEFAULT 0;

UPDATE ad_requests
SET reference_attachment_count = CASE
    WHEN attachment_ids IS NULL OR trim(attachment_ids) = '' THEN 0
    ELSE cardinality(regexp_split_to_array(trim(both from attachment_ids), '\\s*,\\s*'))
END;
