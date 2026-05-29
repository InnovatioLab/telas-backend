UPDATE box_carousel_settings
SET total_slots = 15
WHERE id = 1
  AND total_slots < 15;

UPDATE box_carousel_settings
SET display_duration_ms = 5000
WHERE id = 1
  AND display_duration_ms <> 5000
  AND display_duration_ms > 10000;
