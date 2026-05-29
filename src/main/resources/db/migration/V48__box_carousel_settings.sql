CREATE TABLE box_carousel_settings
(
    id                     SMALLINT PRIMARY KEY CHECK (id = 1),
    display_duration_ms    INTEGER  NOT NULL CHECK (display_duration_ms BETWEEN 1000 AND 60000),
    transition_duration_ms INTEGER  NOT NULL CHECK (transition_duration_ms BETWEEN 0 AND 10000),
    total_slots            SMALLINT NOT NULL CHECK (total_slots BETWEEN 1 AND 30)
);

INSERT INTO box_carousel_settings (id, display_duration_ms, transition_duration_ms, total_slots)
VALUES (1, 5000, 2000, 15);
