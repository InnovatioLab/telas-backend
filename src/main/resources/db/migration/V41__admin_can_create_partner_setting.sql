ALTER TABLE platform_settings
    ADD COLUMN admin_can_create_partner_enabled BOOLEAN NOT NULL DEFAULT FALSE;
