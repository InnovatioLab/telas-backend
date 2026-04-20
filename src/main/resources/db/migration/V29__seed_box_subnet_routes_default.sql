INSERT INTO monitoring.box_subnet_routes (
    id,
    box_id,
    cidr,
    source,
    advertised,
    enabled_route,
    last_synced_at,
    created_at,
    updated_at
)
SELECT gen_random_uuid(),
       b.id,
       '10.0.0.0/24',
       'TAILSCALE',
       true,
       true,
       now(),
       now(),
       now()
FROM public.boxes b
WHERE NOT EXISTS (
    SELECT 1
    FROM monitoring.box_subnet_routes r
    WHERE r.box_id = b.id
);
