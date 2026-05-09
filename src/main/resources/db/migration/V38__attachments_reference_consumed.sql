ALTER TABLE attachments
    ADD COLUMN reference_consumed BOOLEAN NOT NULL DEFAULT FALSE;
