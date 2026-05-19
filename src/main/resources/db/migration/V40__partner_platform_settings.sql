CREATE TABLE platform_settings
(
    id                                  SMALLINT PRIMARY KEY CHECK (id = 1),
    partner_slots_any_location_enabled BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO platform_settings (id, partner_slots_any_location_enabled)
VALUES (1, FALSE);
