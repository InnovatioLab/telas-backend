UPDATE monitors_ads
SET block_quantity = 1
WHERE block_quantity IS DISTINCT FROM 1;
